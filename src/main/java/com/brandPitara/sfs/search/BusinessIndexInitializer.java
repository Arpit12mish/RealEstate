package com.brandPitara.sfs.search;

// package com.brandPitara.sfs.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Component
@RequiredArgsConstructor
public class BusinessIndexInitializer {

    public static final String INDEX_NAME = "sfs-businesses";

    private final ElasticsearchClient esClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws Exception {
        boolean exists = esClient.indices()
                .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                .value();

        if (exists) {
            return;
        }

        String mappingJson = """
        {
          "mappings": {
            "properties": {
              "name":          { "type": "text" },
              "cityId":        { "type": "long" },
              "cityName":      { "type": "text" },
              "categoryId":    { "type": "long" },
              "categoryName":  { "type": "text" },
              "addressText":   { "type": "text" },
              "keywords":      { "type": "text" },
              "location":      { "type": "geo_point" },
              "avgRating":     { "type": "float" },
              "totalRatings":  { "type": "integer" },
              "active":        { "type": "boolean" },
              "sponsored":     { "type": "boolean" },
              "sponsoredPriority": { "type": "integer" }
            }
          }
        }
        """;

        esClient.indices().create(c -> c
                .index(INDEX_NAME)
                .withJson(new StringReader(mappingJson))
        );
    }
}

