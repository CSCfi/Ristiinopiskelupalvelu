package fi.uta.ristiinopiskelu.handler;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;

@TestConfiguration
public abstract class TestEsConfig {

    @Bean
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(EmbeddedElasticsearchInitializer.getElasticHostAddress())
            .build();
    }
}
