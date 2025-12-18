package com.brandPitara.sfs.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.search.model.BusinessSearchDocument;
import com.brandPitara.sfs.service.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessSearchServiceImpl implements BusinessSearchService {

    private final ElasticsearchClient esClient;
    private final BusinessRepository businessRepository;

    @Override
    public void indexBusiness(BusinessEntity entity) {
        BusinessSearchDocument doc = BusinessSearchDocument.fromEntity(entity);

        try {
            esClient.index(i -> i
                    .index(BusinessIndexInitializer.INDEX_NAME)
                    .id(entity.getId().toString())
                    .document(docWithGeo(doc))
            );
            log.info("[ES INDEX] indexed businessId={} index={}", entity.getId(),
                    BusinessIndexInitializer.INDEX_NAME);
        } catch (IOException e) {
            log.error("[ES INDEX] Failed to index businessId={}", entity.getId(), e);
            throw new RuntimeException("Failed to index business " + entity.getId(), e);
        }
    }

    private Map<String, Object> docWithGeo(BusinessSearchDocument doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("name", doc.getName());
        map.put("cityId", doc.getCityId());
        map.put("cityName", doc.getCityName());
        map.put("categoryId", doc.getCategoryId());
        map.put("categoryName", doc.getCategoryName());
        map.put("addressText", doc.getAddressText());
        map.put("keywords", doc.getKeywords());
        if (doc.getLatitude() != null && doc.getLongitude() != null) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("lat", doc.getLatitude());
            loc.put("lon", doc.getLongitude());
            map.put("location", loc);
        }
        map.put("avgRating", doc.getAvgRating());
        map.put("totalRatings", doc.getTotalRatings());
        map.put("active", doc.getActive());
        map.put("sponsored", doc.getSponsored());
        map.put("sponsoredPriority", doc.getSponsoredPriority());
        return map;
    }

    @Override
    public void deleteBusiness(Long businessId) {
        try {
            esClient.delete(d -> d
                    .index(BusinessIndexInitializer.INDEX_NAME)
                    .id(businessId.toString())
            );
            log.info("[ES DELETE] deleted businessId={} from index={}",
                    businessId, BusinessIndexInitializer.INDEX_NAME);
        } catch (IOException e) {
            log.error("[ES DELETE] Failed to delete businessId={} from index",
                    businessId, e);
            throw new RuntimeException("Failed to delete business " + businessId + " from index", e);
        }
    }

    @Override
    public List<BusinessResponse> search(Long cityId,
                                         Long categoryId,
                                         String text,
                                         Double userLat,
                                         Double userLon,
                                         int page,
                                         int size) {

        log.info("[ES SEARCH] cityId={} categoryId={} q='{}' userLat={} userLon={} page={} size={}",
                cityId, categoryId, text, userLat, userLon, page, size);

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        // 🔍 full-text
        if (StringUtils.hasText(text)) {
            must.add(Query.of(q -> q.multiMatch(m -> m
                    .query(text)
                    // ✅ pass fields as separate entries instead of one comma-separated string
                    .fields("name^3", "categoryName^2", "cityName", "addressText", "keywords")
                    .fuzziness("AUTO")
            )));
        }

        // 🔎 filters
        if (cityId != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("cityId").value(cityId))));
        }
        if (categoryId != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("categoryId").value(categoryId))));
        }

        // 🧮 sort: sponsored first, then geo-distance (if provided)
        List<SortOptions> sort = new ArrayList<>();
        sort.add(SortOptions.of(s -> s
                .field(f -> f
                        .field("sponsoredPriority")
                        .order(SortOrder.Desc)
                )
        ));

        if (userLat != null && userLon != null) {
            sort.add(SortOptions.of(s -> s
                    .geoDistance(g -> g
                            .field("location")
                            .location(l -> l.latlon(ll -> ll.lat(userLat).lon(userLon)))
                            .order(SortOrder.Asc)
                    )
            ));
        }

        SearchResponse<BusinessSearchDocument> response;
        try {
            response = esClient.search(s -> s
                            .index(BusinessIndexInitializer.INDEX_NAME)
                            .query(q -> q.bool(b -> {
                                if (!must.isEmpty()) {
                                    b.must(must);
                                }
                                if (!filter.isEmpty()) {
                                    b.filter(filter);
                                }
                                return b;
                            }))
                            .from(page * size)
                            .size(size)
                            .sort(sort),
                    BusinessSearchDocument.class);

            log.info("[ES SEARCH] took={}ms totalHits={} shards={}",
                    response.took(),
                    response.hits().total() != null ? response.hits().total().value() : null,
                    response.shards() != null ? response.shards().total() : null
            );

        } catch (Exception e) {
            log.error("[ES SEARCH] Elasticsearch search failed. cityId={} categoryId={} q='{}' userLat={} userLon={} page={} size={}",
                    cityId, categoryId, text, userLat, userLon, page, size, e);
            throw new RuntimeException("Failed to search businesses", e);
        }

        List<Long> ids = response.hits().hits().stream()
                .map(h -> Long.valueOf(h.id()))
                .toList();

        if (ids.isEmpty()) {
            log.info("[ES SEARCH] no hits found");
            return List.of();
        }

        List<BusinessEntity> entities = businessRepository.findAllById(ids);

        Map<Long, BusinessEntity> map = new HashMap<>();
        for (BusinessEntity e : entities) {
            map.put(e.getId(), e);
        }

        List<BusinessResponse> result = new ArrayList<>();
        for (Long id : ids) {
            BusinessEntity e = map.get(id);
            if (e != null) {
                result.add(BusinessMapper.toResponse(e));
            }
        }

        log.info("[ES SEARCH] returning {} results to client", result.size());
        return result;
    }
}
