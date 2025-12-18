package com.brandPitara.sfs.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// Optional: only run this in a special profile, not always
@Profile("es-smoke")
@RequiredArgsConstructor
public class ElasticsearchSmokeTest implements CommandLineRunner {

    private final ElasticsearchClient esClient;

    @Override
    public void run(String... args) {
        try {
            log.info("[ES SMOKE] Pinging Elasticsearch...");

            // simplest: just call info()
            esClient.info();

            log.info("[ES SMOKE] ✅ Elasticsearch is reachable.");
        } catch (Exception e) {
            // ❗ DO NOT rethrow – just log
            log.warn("[ES SMOKE] ⚠️ Elasticsearch is NOT reachable at startup. "
                    + "Skipping smoke test. App will still start.", e);
        }
    }
}
