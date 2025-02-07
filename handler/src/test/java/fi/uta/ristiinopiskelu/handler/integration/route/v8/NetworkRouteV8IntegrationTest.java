package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class NetworkRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkRouteV8IntegrationTest.class);

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(8000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.artemis.admin-ui-user}")
    private String adminUiUser;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    private int previousMessageSchemaVersion = 8;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
    }

    @Test
    public void testSendingCreateNetworkMessage_shouldSuccess() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.previousMessageSchemaVersion);

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            NetworkOrganisation networkOrg = new NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrg.setIsCoordinator(true);
            networkOrg.setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now(), OffsetDateTime.now().plusYears(1)));
            networkOrganisations.add(networkOrg);
        }

        NetworkWriteDTO network = DtoInitializer.getNetwork("TESTIVERKOSTO", new LocalisedString("verkosto", "verkosto en", "verkosto sv"),
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()), networkOrganisations);

        CreateNetworkRequest req = new CreateNetworkRequest();
        req.setNetwork(network);

        Message responseMessage = JmsHelper.sendAndReceiveObjectWithUserId(jmsTemplate, req, adminUiUser);
        Object received = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        if(!(received instanceof DefaultResponse)) {
            fail("Response message should be DefaultResponse. Got:" + received.getClass());
        }

        DefaultResponse response = (DefaultResponse) received;
        assertEquals(Status.OK, response.getStatus());
        assertNotNull(response.getMessage());

        Message m1 = jmsTemplate.receive("org_network_queue_1");
        assertEquals(m1.getStringProperty("messageType"), fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST.name());

        fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest resp2 =
            (fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST.name());

        fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest resp3 =
            (fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest) jmsTemplate.getMessageConverter().fromMessage(m2);
        assertEquals(resp3.getNetwork().getId(), "TESTIVERKOSTO");

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        assertEquals("TESTIVERKOSTO", result.getId());
    }

    @Test
    public void testSendingCreateNetworkMessageWithMissingCodeSet_shouldFail() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.previousMessageSchemaVersion);

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            NetworkOrganisation networkOrg = new NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrg.setIsCoordinator(true);
            networkOrg.setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now(), OffsetDateTime.now().plusYears(1)));
            networkOrganisations.add(networkOrg);
        }

        NetworkWriteDTO network = DtoInitializer.getNetwork("TESTIVERKOSTO", new LocalisedString("verkosto", "verkosto en", "verkosto sv"),
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()), networkOrganisations);

        network.setTargetGroups(Collections.singletonList(new CodeReference("outo_koodiarvo", "outo_koodisto")));

        CreateNetworkRequest req = new CreateNetworkRequest();
        req.setNetwork(network);

        Message responseMessage = JmsHelper.sendAndReceiveObjectWithUserId(jmsTemplate, req, adminUiUser);
        Object received = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        if(!(received instanceof DefaultResponse)) {
            fail("Response message should be DefaultResponse. Got:" + received.getClass());
        }

        DefaultResponse response = (DefaultResponse) received;
        assertEquals(Status.FAILED, response.getStatus());
        assertNotNull(response.getMessage());

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(0, savedNetworks.size());
    }

    @Test
    public void testSendingCreateNetworkMessageAsJson_ShouldSuccess() throws JMSException {
        List<String> organisationIds = Arrays.asList("UEF", "HAAGAH", "JUY", "LAUREA", "METROP", "SAV", "TUNI");
        createOrganisations(organisationIds, this.previousMessageSchemaVersion);

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, createNetworkJson, MessageType.CREATE_NETWORK_REQUEST.name(), adminUiUser);
        Object received = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        if(!(received instanceof DefaultResponse)) {
            fail("Response message should be DefaultResponse. Got:" + received.getClass());
        }

        DefaultResponse response = (DefaultResponse) received;
        assertEquals(Status.OK, response.getStatus());
        assertNotNull(response.getMessage());

        for(int i=1; i <= organisationIds.size(); i++) {
            Message m1 = jmsTemplate.receive("org_network_queue_" + i);
            assertEquals(m1.getStringProperty("messageType"), fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST.name());

            fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest resp2 =
                (fi.uta.ristiinopiskelu.messaging.message.v8.network.CreateNetworkRequest) jmsTemplate.getMessageConverter().fromMessage(m1);
            assertEquals(resp2.getNetwork().getId(), "CN-1");
        }

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        organisationIds.stream().forEach(id -> assertTrue(result.getOrganisations().stream().anyMatch(o -> o.getOrganisationTkCode().equals(id))));
        assertEquals("CN-1", result.getId());
        assertEquals(7, result.getOrganisations().size());
    }

    @Test
    public void testSendingCreateNetworkMessage_shouldFailOrganisationsNotFound() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.previousMessageSchemaVersion);

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : Arrays.asList("ORG3", "ORG4")) {
            NetworkOrganisation networkOrg = new NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrg.setOrganisationTkCode(orgId + "-TK");
            networkOrg.setIsCoordinator(true);
            networkOrg.setValidityInNetwork(DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()));
            networkOrganisations.add(networkOrg);
        }

        NetworkWriteDTO network = DtoInitializer.getNetwork("TESTIVERKOSTO", new LocalisedString("verkosto", null, null),
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()), networkOrganisations);

        CreateNetworkRequest req = new CreateNetworkRequest();
        req.setNetwork(network);

        // Send create network message
        Message responseMessage = JmsHelper.sendAndReceiveObjectWithUserId(jmsTemplate, req, adminUiUser);
        Object received = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        if(!(received instanceof DefaultResponse)) {
            fail("Response message should be DefaultResponse. Got:" + received.getClass());
        }

        DefaultResponse response = (DefaultResponse) received;
        assertEquals(Status.FAILED, response.getStatus());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("ORG3"));
        assertTrue(response.getMessage().contains("ORG4"));

        // Verify that destination queues are empty
        jmsTemplate.setReceiveTimeout(1000); // No need to wait for default time
        Message m1 = jmsTemplate.receive("org_network_queue_1");
        Assert.isNull(m1, "message should be null");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        Assert.isNull(m2, "message should be null");

        // Verify that network is not written to elasticsearch
        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(0, savedNetworks.size());
    }

    @Test
    public void testSendingUpdateNetworkMessage_shouldSuccess() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.previousMessageSchemaVersion);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("testiverkostonimi", null, null);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("TESTIVERKOSTO", originalName, null, null, true);

        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation networkOrg =
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrganisations.add(networkOrg);
        }

        networkEntity.setOrganisations(networkOrganisations);

        networkRepository.create(networkEntity);

        String updateJson =
                "{\n" +
                "\t\"network\": {\n" +
                "\t\t\"id\": \"TESTIVERKOSTO\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"Uusi verkosto nimi\",\n" +
                "\t\t\t\t\"en\": \"Network name 2\",\n" +
                "\t\t\t\t\"sv\": \"Network namn 2\"\n" +
                "\t\t  }\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        Message m1 = jmsTemplate.receive("org_network_queue_1");
        assertEquals(m1.getStringProperty("messageType"), fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_NETWORK_REQUEST.name());

        fi.uta.ristiinopiskelu.messaging.message.v8.network.UpdateNetworkRequest resp2 =
            (fi.uta.ristiinopiskelu.messaging.message.v8.network.UpdateNetworkRequest) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");
        assertEquals(resp2.getNetwork().getName().getValue("fi"), "Uusi verkosto nimi");
        assertEquals(resp2.getNetwork().getName().getValue("en"), "Network name 2");
        assertEquals(resp2.getNetwork().getName().getValue("sv"), "Network namn 2");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_NETWORK_REQUEST.name());
        
        fi.uta.ristiinopiskelu.messaging.message.v8.network.UpdateNetworkRequest resp3 =
            (fi.uta.ristiinopiskelu.messaging.message.v8.network.UpdateNetworkRequest) jmsTemplate.getMessageConverter().fromMessage(m2);
        assertEquals(resp3.getNetwork().getId(), "TESTIVERKOSTO");
        assertEquals(resp3.getNetwork().getName().getValue("fi"), "Uusi verkosto nimi");
        assertEquals(resp3.getNetwork().getName().getValue("en"), "Network name 2");
        assertEquals(resp3.getNetwork().getName().getValue("sv"), "Network namn 2");

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        assertEquals("TESTIVERKOSTO", result.getId());
        assertEquals(result.getName().getValue("fi"), "Uusi verkosto nimi");
        assertEquals(result.getName().getValue("en"), "Network name 2");
        assertEquals(result.getName().getValue("sv"), "Network namn 2");
        assertNotNull(result.getOrganisations());
        assertEquals(result.getOrganisations().size(), 2);
    }

    @Test
    public void testSendingCreateNetwork_shouldFailMissingRequiredFields() throws JMSException {
        String createJson =
                "{\n" +
                "\t\"network\": {\n" +
                "\t\t\"id\": \"TESTIVERKOSTO\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"Uusi verkosto nimi\",\n" +
                "\t\t\t\t\"en\": \"Network name 2\",\n" +
                "\t\t\t\t\"sv\": \"Network namn 2\"\n" +
                "\t\t  }\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, createJson, MessageType.CREATE_NETWORK_REQUEST.name(), adminUiUser);
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);
        assertEquals(4, response.getErrors().size());
    }

    @Test
    public void testSendingUpdateNetwork_shouldFailMissingRequiredFields() throws JMSException {
        String updateJson =
                "{\n" +
                "\t\"network\": {\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"Uusi verkosto nimi\",\n" +
                "\t\t\t\t\"en\": \"Network name 2\",\n" +
                "\t\t\t\t\"sv\": \"Network namn 2\"\n" +
                "\t\t  }\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);
        assertEquals(1, response.getErrors().size());
    }

    private void createOrganisations(List<String> organisationIds, int messageSchemaVersion) {
        for(int i = 1; i <= organisationIds.size(); i++) {
            OrganisationEntity orgEnt = EntityInitializer.getOrganisationEntity(organisationIds.get(i - 1),
                "org_network_queue_"+i, null, messageSchemaVersion);
            organisationRepository.create(orgEnt);
        }
    }

    private final String createNetworkJson =
            "{\n" +
            "  \"network\": {\n" +
            "   \"id\": \"CN-1\",\n" +
            "   \"name\": {\n" +
            "     \"values\": {\n" +
            "       \"fi\": \"Verkosto 1\",\n" +
            "       \"en\": \"Network 1\",\n" +
            "       \"sv\": null\n" +
            "     }\n" +
            "   },\n" +
            "   \"published\": true,\n" +
            "   \"abbreviation\": \"V1\",\n" +
            "   \"networkType\": \"CURRICULUM_LEVEL\",\n" +
            "   \"organisations\": [\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"UEF\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.587+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"HAAGAH\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.589+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"JUY\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.590+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"LAUREA\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.590+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"METROP\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.590+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"SAV\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.590+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     },\n" +
            "     {\n" +
            "       \"organisationTkCode\": \"TUNI\",\n" +
            "       \"isCoordinator\": true,\n" +
            "       \"validityInNetwork\": {\n" +
            "         \"continuity\": \"INDEFINITELY\",\n" +
            "         \"start\": \"2019-05-03T15:33:52.590+03:00\",\n" +
            "         \"end\": null\n" +
            "       }\n" +
            "     }\n" +
            "   ],\n" +
            "   \"validity\": {\n" +
            "     \"continuity\": \"INDEFINITELY\",\n" +
            "     \"start\": \"2019-01-01T00:00:00.000+03:00\",\n" +
            "     \"end\": null\n" +
            "   },\n" +
            "   \"description\": {\n" +
            "     \"values\": {\n" +
            "       \"fi\": \"Verkosto, joka sisältää kaikki organisaatiot. Kaikki organisaatiot ovat organisoijia.\",\n" +
            "       \"en\": \"Network that contains all organisations. All organisations are coordinators.\",\n" +
            "       \"sv\": null\n" +
            "     }\n" +
            "   }\n" +
            " }\n" +
            "}";
}
