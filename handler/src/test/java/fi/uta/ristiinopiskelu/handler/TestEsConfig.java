package fi.uta.ristiinopiskelu.handler;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;

@TestConfiguration
public abstract class TestEsConfig {

    @Primary
    @Bean
    public ElasticsearchClient elasticsearchClient(ObjectMapper objectMapper) {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
            .connectedTo(EmbeddedElasticsearchInitializer.getElasticHostAddress())
            .build();

        RestClient restClient = ElasticsearchClients.getRestClient(clientConfiguration);
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

        return new ElasticsearchClient(transport);
    }
}

