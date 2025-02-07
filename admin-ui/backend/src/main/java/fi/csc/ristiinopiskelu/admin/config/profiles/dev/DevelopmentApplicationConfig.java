package fi.csc.ristiinopiskelu.admin.config.profiles.dev;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import fi.csc.ristiinopiskelu.admin.config.AdminUiMapperConfig;
import fi.csc.ristiinopiskelu.admin.config.ApplicationConfig;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.UserRoleEntity;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.MessageSchemaRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.*;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
            .withTag("8.10.4"));
        embeddedElastic.addEnv("cluster.name", "ripa-cluster");
        embeddedElastic.addEnv("xpack.security.enabled", "false");
        embeddedElastic.addEnv("indices.id_field_data.enabled", "true");
        embeddedElastic.start();
        return embeddedElastic;
    }

    @ConditionalOnExpression(EMBEDDED_ES_ENABLED_SPEL)
    @Primary
    @Bean
    public ElasticsearchClient client(ElasticsearchContainer embeddedElastic) {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
            .connectedTo("localhost:"+ embeddedElastic.getMappedPort(9200))
            .build();
        return ElasticsearchClients.createImperative(clientConfiguration);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if(!embeddedEsEnabled) {
            return;
        }

        ElasticsearchTemplate elasticsearchRestTemplate = applicationReadyEvent.getApplicationContext().getBean(ElasticsearchTemplate.class);
        OrganisationRepository organisationRepository = applicationReadyEvent.getApplicationContext().getBean(OrganisationRepository.class);
        MessageSchemaRepository messageSchemaRepository = applicationReadyEvent.getApplicationContext().getBean(MessageSchemaRepository.class);
        UserRoleRepository userRoleRepository = applicationReadyEvent.getApplicationContext().getBean(UserRoleRepository.class);
        NetworkRepository networkRepository = applicationReadyEvent.getApplicationContext().getBean(NetworkRepository.class);

        List<String> indices = Arrays.asList("koulut", "verkostot", "koodisto", "kayttajaroolit", "viestiskeemat");

        LOGGER.info("Creating ES indices [{}] for development use...", StringUtils.collectionToCommaDelimitedString(indices));
        createIndices(indices, elasticsearchRestTemplate);

        LOGGER.info("Creating base sample data for development use...");
        createBaseData(organisationRepository, messageSchemaRepository, userRoleRepository, networkRepository);
        LOGGER.info("Done");
    }

    private void createBaseData(OrganisationRepository organisationRepository, MessageSchemaRepository messageSchemaRepository,
                                UserRoleRepository userRoleRepository, NetworkRepository networkRepository) {
        OrganisationEntity organisationEntityTuni = new OrganisationEntity();
        organisationEntityTuni.setId("DEV10122");
        organisationEntityTuni.setOrganisationName(new LocalisedString("Tampereen yliopisto", null, null));
        organisationEntityTuni.setAdministratorEmail("ripa@gofore.com");
        organisationEntityTuni.setQueue("queue:tuni");
        organisationEntityTuni.setSchemaVersion(9);
        organisationRepository.create(organisationEntityTuni);

        OrganisationEntity organisationEntityLapland = new OrganisationEntity();
        organisationEntityLapland.setId("DEV10108");
        organisationEntityLapland.setOrganisationName(new LocalisedString("Lapin ammattikorkeakoulu", null, null));
        organisationEntityLapland.setAdministratorEmail("ripa@gofore.com");
        organisationEntityLapland.setQueue("queue:lapinamk");
        organisationEntityLapland.setSchemaVersion(9);
        organisationRepository.create(organisationEntityLapland);

        OrganisationEntity organisationEntityTurku = new OrganisationEntity();
        organisationEntityTurku.setId("DEV02509");
        organisationEntityTurku.setOrganisationName(new LocalisedString("Turun ammattikorkeakoulu", null, null));
        organisationEntityTurku.setAdministratorEmail("ripa@gofore.com");
        organisationEntityTurku.setQueue("queue:turkuamk");
        organisationEntityTurku.setSchemaVersion(9);
        organisationRepository.create(organisationEntityTurku);

        OrganisationEntity organisationEntityLaurea = new OrganisationEntity();
        organisationEntityLaurea.setId("DEV02629");
        organisationEntityLaurea.setOrganisationName(new LocalisedString("Laurea-ammattikorkeakoulu", null, null));
        organisationEntityLaurea.setAdministratorEmail("ripa@gofore.com");
        organisationEntityLaurea.setQueue("queue:laurea");
        organisationEntityLaurea.setSchemaVersion(9);
        organisationRepository.create(organisationEntityLaurea);

        MessageSchemaEntity messageSchemaV8Entity = new MessageSchemaEntity();
        messageSchemaV8Entity.setSchemaVersion(8);
        messageSchemaRepository.create(messageSchemaV8Entity);

        MessageSchemaEntity messageSchemaV9Entity = new MessageSchemaEntity();
        messageSchemaV9Entity.setSchemaVersion(9);
        messageSchemaRepository.create(messageSchemaV9Entity);

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setEppn("oo99999@tuni.fi");
        userRoleEntity.setRole(Role.SUPERUSER);
        userRoleEntity.setOrganisation("DEV10122"); // TUNI
        userRoleRepository.create(userRoleEntity);

        NetworkEntity networkEntity1 = new NetworkEntity();
        networkEntity1.setId(UUID.randomUUID().toString());
        networkEntity1.setName(new LocalisedString("Verkosto 1", "First network", null));
        networkEntity1.setAbbreviation("VERK1");
        networkEntity1.setNetworkType(NetworkType.CURRICULUM_LEVEL);
        networkEntity1.setDescription(new LocalisedString("Testiverkosto 1: Tampereen yliopisto ja Laurea-ammattikorkeakoulu", null, null));
        networkEntity1.setPublished(false);
        Validity net1Validity = new Validity();
        net1Validity.setContinuity(Validity.ContinuityEnum.INDEFINITELY);
        net1Validity.setStart(OffsetDateTime.now());
        networkEntity1.setValidity(net1Validity);
        NetworkOrganisation net1OrgTuni = new NetworkOrganisation();
        net1OrgTuni.setIsCoordinator(true);
        net1OrgTuni.setOrganisationTkCode(organisationEntityTuni.getId());
        net1OrgTuni.setValidityInNetwork(net1Validity);
        NetworkOrganisation net1OrgLaurea = new NetworkOrganisation();
        net1OrgLaurea.setIsCoordinator(false);
        net1OrgLaurea.setOrganisationTkCode(organisationEntityLaurea.getId());
        net1OrgLaurea.setValidityInNetwork(net1Validity);
        networkEntity1.setOrganisations(List.of(net1OrgTuni, net1OrgLaurea));
        networkRepository.create(networkEntity1);

        NetworkEntity networkEntity2 = new NetworkEntity();
        networkEntity2.setId(UUID.randomUUID().toString());
        networkEntity2.setName(new LocalisedString("Verkosto 2", "Second network", null));
        networkEntity2.setAbbreviation("VERK2");
        networkEntity2.setNetworkType(NetworkType.CURRICULUM_LEVEL);
        networkEntity2.setDescription(new LocalisedString("Testiverkosto 2: Tampereen yliopisto ja Turun ammattikorkeakoulu", null, null));
        networkEntity2.setPublished(false);
        Validity net2Validity = new Validity();
        net2Validity.setContinuity(Validity.ContinuityEnum.FIXED);
        net2Validity.setStart(OffsetDateTime.now());
        net2Validity.setEnd(OffsetDateTime.now().plusMonths(6));
        networkEntity2.setValidity(net2Validity);
        NetworkOrganisation net2OrgTuni = new NetworkOrganisation();
        net2OrgTuni.setIsCoordinator(true);
        net2OrgTuni.setOrganisationTkCode(organisationEntityTuni.getId());
        net2OrgTuni.setValidityInNetwork(net2Validity);
        NetworkOrganisation net2OrgTurku = new NetworkOrganisation();
        net2OrgTurku.setIsCoordinator(false);
        net2OrgTurku.setOrganisationTkCode(organisationEntityTurku.getId());
        net2OrgTurku.setValidityInNetwork(net2Validity);
        networkEntity2.setOrganisations(List.of(net2OrgTuni, net2OrgTurku));
        networkRepository.create(networkEntity2);
    }

    private void createIndices(List<String> indices, ElasticsearchTemplate template) {
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
