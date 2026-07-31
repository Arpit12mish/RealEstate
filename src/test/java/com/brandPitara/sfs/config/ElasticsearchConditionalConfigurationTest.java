package com.brandPitara.sfs.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.brandPitara.sfs.search.BusinessIndexInitializer;
import com.brandPitara.sfs.search.gateway.BusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.impl.DisabledBusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.impl.ElasticsearchBusinessSearchGateway;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchConditionalConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ElasticsearchConfig.class,
                    ElasticsearchBusinessSearchGateway.class,
                    DisabledBusinessSearchGateway.class,
                    BusinessIndexInitializer.class
            );

    @Test
    void disabledSearchCreatesNoElasticsearchClientAndUsesDisabledGateway() {
        contextRunner.withPropertyValues("sfs.search.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RestClient.class);
                    assertThat(context).doesNotHaveBean(ElasticsearchClient.class);
                    assertThat(context).hasSingleBean(BusinessSearchGateway.class);
                    assertThat(context).hasSingleBean(DisabledBusinessSearchGateway.class);
                    assertThat(context).doesNotHaveBean(ElasticsearchBusinessSearchGateway.class);
                    assertThat(context).doesNotHaveBean(BusinessIndexInitializer.class);
                });
    }

    @Test
    void enabledSearchCreatesRealClientAndGateway() {
        contextRunner.withPropertyValues(
                        "sfs.search.enabled=true",
                        "elasticsearch.url=http://127.0.0.1:9200",
                        "elasticsearch.api-key=test-only-not-real"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RestClient.class);
                    assertThat(context).hasSingleBean(ElasticsearchClient.class);
                    assertThat(context).hasSingleBean(BusinessSearchGateway.class);
                    assertThat(context).hasSingleBean(ElasticsearchBusinessSearchGateway.class);
                    assertThat(context).doesNotHaveBean(DisabledBusinessSearchGateway.class);
                    assertThat(context).hasSingleBean(BusinessIndexInitializer.class);
                });
    }
}
