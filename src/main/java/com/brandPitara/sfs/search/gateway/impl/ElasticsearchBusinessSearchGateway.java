package com.brandPitara.sfs.search.gateway.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.brandPitara.sfs.search.BusinessIndexInitializer;
import com.brandPitara.sfs.search.gateway.BusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.BusinessSearchQuery;
import com.brandPitara.sfs.search.model.BusinessSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place ElasticsearchClient is used for business search. Exceptions
 * propagate unchanged (never caught/logged here) - BusinessSearchServiceImpl
 * owns the "never crash the request, fall back to DB" decision, exactly as
 * it did before this gateway existed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchBusinessSearchGateway implements BusinessSearchGateway {

    private final ElasticsearchClient esClient;

    @Override
    public void indexBusiness(BusinessSearchDocument doc) throws Exception {
        esClient.index(i -> i
                .index(BusinessIndexInitializer.INDEX_NAME)
                .id(doc.getId().toString())
                .document(docWithGeo(doc))
        );
        log.info("[ES INDEX] indexed businessId={} index={}", doc.getId(), BusinessIndexInitializer.INDEX_NAME);
    }

    @Override
    public void deleteBusiness(Long businessId) throws Exception {
        esClient.delete(d -> d
                .index(BusinessIndexInitializer.INDEX_NAME)
                .id(businessId.toString())
        );
        log.info("[ES DELETE] deleted businessId={} from index={}", businessId, BusinessIndexInitializer.INDEX_NAME);
    }

    @Override
    public List<Long> search(BusinessSearchQuery query) throws Exception {
        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        if (StringUtils.hasText(query.text())) {
            must.add(Query.of(q -> q.multiMatch(m -> m
                    .query(query.text())
                    .fields("name^3", "categoryName^2", "cityName", "addressText", "keywords")
                    .fuzziness("AUTO")
            )));
        }

        if (query.cityId() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("cityId").value(query.cityId()))));
        }
        if (query.categoryId() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("categoryId").value(query.categoryId()))));
        }

        List<SortOptions> sort = new ArrayList<>();
        sort.add(SortOptions.of(s -> s.field(f -> f.field("sponsoredPriority").order(SortOrder.Desc))));

        if (query.userLat() != null && query.userLon() != null) {
            sort.add(SortOptions.of(s -> s.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(query.userLat()).lon(query.userLon())))
                    .order(SortOrder.Asc)
            )));
        }

        SearchResponse<BusinessSearchDocument> response = esClient.search(s -> s
                        .index(BusinessIndexInitializer.INDEX_NAME)
                        .query(q -> q.bool(b -> {
                            if (!must.isEmpty()) b.must(must);
                            if (!filter.isEmpty()) b.filter(filter);
                            return b;
                        }))
                        .from(query.page() * query.size())
                        .size(query.size())
                        .sort(sort),
                BusinessSearchDocument.class);

        log.info("[ES SEARCH] took={}ms totalHits={}",
                response.took(),
                response.hits().total() != null ? response.hits().total().value() : null);

        return response.hits().hits().stream()
                .map(h -> Long.valueOf(h.id()))
                .toList();
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
}
