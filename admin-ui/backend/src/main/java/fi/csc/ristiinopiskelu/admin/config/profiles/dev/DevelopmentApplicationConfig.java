package fi.csc.ristiinopiskelu.admin.config.profiles.dev;

import fi.csc.ristiinopiskelu.admin.config.AdminUiMapperConfig;
import fi.csc.ristiinopiskelu.admin.config.ApplicationConfig;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.MessageSchemaRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.*;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Profile("dev")
@Configuration
@Import({EsConfig.class, AdminUiMapperConfig.class, MessagingConfig.class})
public class DevelopmentApplicationConfig extends ApplicationConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevelopmentApplicationConfig.class);
    private static final String EMBEDDED_ES_ENABLED_SPEL = "${general.elasticsearch.embedded:true}";

    @Value(EMBEDDED_ES_ENABLED_SPEL)
    private boolean embeddedEsEnabled = true;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       // noop
    }

    @ConditionalOnExpression(EMBEDDED_ES_ENABLED_SPEL)
    @Bean(destroyMethod = "stop")
    public ElasticsearchContainer embeddedElastic() throws Exception {
        ElasticsearchContainer embeddedElastic = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("7.17.4"));
        embeddedElastic.addEnv("cluster.name", "ripa-cluster");
        embeddedElastic.addEnv("xpack.security.enabled", "false");
        embeddedElastic.start();
        return embeddedElastic;
    }

    @ConditionalOnExpression(EMBEDDED_ES_ENABLED_SPEL)
    @Primary
    @Bean
    public RestHighLevelClient client(ElasticsearchContainer embeddedElastic) {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
            .connectedTo("localhost:"+ embeddedElastic.getMappedPort(9200))
            .build();
        return RestClients.create(clientConfiguration).rest();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if(!embeddedEsEnabled) {
            return;
        }

        ElasticsearchRestTemplate elasticsearchRestTemplate = applicationReadyEvent.getApplicationContext().getBean(ElasticsearchRestTemplate.class);
        OrganisationRepository organisationRepository = applicationReadyEvent.getApplicationContext().getBean(OrganisationRepository.class);
        MessageSchemaRepository messageSchemaRepository = applicationReadyEvent.getApplicationContext().getBean(MessageSchemaRepository.class);

        List<String> indices = Arrays.asList("koulut", "verkostot", "koodisto", "kayttajaroolit", "viestiskeemat");

        LOGGER.info("Creating ES indices [{}] for development use...", StringUtils.collectionToCommaDelimitedString(indices));
        createIndices(indices, elasticsearchRestTemplate);

        LOGGER.info("Creating base data (TUNI organisation, message schema version) for development use...");
        createBaseData(organisationRepository, messageSchemaRepository);
    }

    private void createBaseData(OrganisationRepository organisationRepository, MessageSchemaRepository messageSchemaRepository) {
        OrganisationEntity organisationEntity = new OrganisationEntity();
        organisationEntity.setId("TUNI");
        organisationEntity.setOrganisationName(new LocalisedString("Tampereen yliopisto", null, null));
        organisationEntity.setAdministratorEmail("ripa@gofore.com");
        organisationEntity.setQueue("queue:tuni");
        organisationEntity.setSchemaVersion(8);
        organisationRepository.create(organisationEntity);

        MessageSchemaEntity messageSchemaEntity = new MessageSchemaEntity();
        messageSchemaEntity.setSchemaVersion(8);
        messageSchemaRepository.create(messageSchemaEntity);
    }

    private void createIndices(List<String> indices, ElasticsearchRestTemplate template) {
        for(String index : indices) {
            IndexOperations indexOperations = template.indexOps(IndexCoordinates.of(index));

            Settings parsedSettings = null;
            Document parsedMapper = null;

            try(InputStream settings = this.getClass().getResourceAsStream(String.format("/settings/%s.json", index));
                InputStream mapper = this.getClass().getResourceAsStream(String.format("/mappers/%s.json", index))) {
                if (settings != null && settings.available() > 0) {
                    parsedSettings = Settings.parse(new String(settings.readAllBytes()));
                }

                if (mapper != null && mapper.available() > 0) {
                    parsedMapper = Document.parse(new String(mapper.readAllBytes()));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read settings/mapper files for index " + index, e);
            }

            if(parsedMapper == null) {
                throw new IllegalStateException("No mapper file parsed for index " + index);
            }

            if(parsedSettings == null) {
                parsedSettings = new Settings();
            }

            boolean result = indexOperations.create(parsedSettings, parsedMapper);
            if(!result) {
                throw new IllegalStateException("Index " + index + " could not be created");
            }
        }
    }
}
