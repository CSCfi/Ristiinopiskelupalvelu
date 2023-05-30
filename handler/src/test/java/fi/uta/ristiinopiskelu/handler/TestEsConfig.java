package fi.uta.ristiinopiskelu.handler;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

@TestConfiguration
public abstract class TestEsConfig {

    @Primary
    @Bean
    public RestHighLevelClient client(){

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
            .connectedTo(EmbeddedElasticsearchInitializer.getElasticHostAddress())
            .build();
        return RestClients.create(clientConfiguration).rest();
    }
}

