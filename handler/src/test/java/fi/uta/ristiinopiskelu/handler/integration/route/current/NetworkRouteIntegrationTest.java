package fi.uta.ristiinopiskelu.handler.integration.route.current;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
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
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkCreatedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkUpdatedNotification;
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
import tools.jackson.databind.json.JsonMapper;

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
public class NetworkRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkRouteIntegrationTest.class);

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
    private JsonMapper jsonMapper;

    @Value("${spring.artemis.admin-ui-user}")
    private String adminUiUser;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
    }

    @Test
    public void testSendingCreateNetworkMessage_shouldSucceed() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());
        
        NetworkCreatedNotification resp3 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m2);
        assertEquals(resp3.getNetwork().getId(), "TESTIVERKOSTO");

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        assertEquals("TESTIVERKOSTO", result.getId());
        assertTrue(result.isPublished());
    }

    @Test
    public void testSendingCreateNetworkMessage_htmlSanitizedAndUnencoded_shouldSucceed() throws JMSException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            NetworkOrganisation networkOrg = new NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrg.setIsCoordinator(true);
            networkOrg.setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now(), OffsetDateTime.now().plusYears(1)));
            networkOrganisations.add(networkOrg);
        }

        NetworkWriteDTO network = DtoInitializer.getNetwork("TESTIVERKOSTO", new LocalisedString("verkosto & enkoodaus",
                "verkosto en & encoding", "verkosto sv & enkood"),
            DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()), networkOrganisations);
        network.setDescription(new LocalisedString("<script>alert('häly');</script>Joku <p><strong>TOSI ISO kuvaus\\ <xc</strong></p>", null, null));

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
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp3 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m2);
        assertEquals(resp3.getNetwork().getId(), "TESTIVERKOSTO");

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false)
            .toList();
        assertNotNull(savedNetworks);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.getFirst();
        assertEquals("TESTIVERKOSTO", result.getId());
        assertEquals("verkosto & enkoodaus", result.getName().getValue(Language.FI));
        assertEquals("Joku <p>TOSI ISO kuvaus\\ </p>", result.getDescription().getValue(Language.FI));
        assertTrue(result.isPublished());
    }

    @Test
    public void testSendingCreateNetworkMessage_notificationsShouldNotBeSentIfNetworkIsNotPublished_shouldSucceed() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
        network.setPublished(false);

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
        assertNull(m1);

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertNull(m2);

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        assertEquals("TESTIVERKOSTO", result.getId());
        assertFalse(result.isPublished());
    }

    @Test
    public void testSendingCreateNetworkMessageReceivingOrganisationHasOlderVersion_shouldSucceed() throws JMSException, IOException {
        OrganisationEntity orgEnt1 = EntityInitializer.getOrganisationEntity("ORG1", "org_network_queue_1", null, this.messageSchemaVersion);
        organisationRepository.create(orgEnt1);

        OrganisationEntity orgEnt2 = EntityInitializer.getOrganisationEntity("ORG2", "org_network_queue_2", null, this.messageSchemaVersion - 1);
        organisationRepository.create(orgEnt2);

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : Arrays.asList("ORG1", "ORG2")) {
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
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
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
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
    public void testSendingCreateNetworkMessageAsJson_shouldSucceed() throws JMSException {
        List<String> organisationIds = Arrays.asList("UEF", "HAAGAH", "JUY", "LAUREA", "METROP", "SAV", "TUNI");
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
            assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

            NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
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
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
    public void testSendingCreateNetworkMessage_withSameIdMultipleTimes_shouldFail() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());
        
        NetworkCreatedNotification resp3 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m2);
        assertEquals(resp3.getNetwork().getId(), "TESTIVERKOSTO");

        // send the same message again, this time it should fail
        responseMessage = JmsHelper.sendAndReceiveObjectWithUserId(jmsTemplate, req, adminUiUser);
        received = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        if(!(received instanceof DefaultResponse)) {
            fail("Response message should be DefaultResponse. Got:" + received.getClass());
        }

        response = (DefaultResponse) received;
        assertEquals(Status.FAILED, response.getStatus());
        assertNotNull(response.getMessage());

        m1 = jmsTemplate.receive("org_network_queue_1");
        assertNull(m1);

        m2 = jmsTemplate.receive("org_network_queue_2");
        assertNull(m2);

        List<NetworkEntity> savedNetworks = StreamSupport.stream(networkRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedNetworks != null);
        assertEquals(1, savedNetworks.size());

        NetworkEntity result = savedNetworks.get(0);
        assertEquals("TESTIVERKOSTO", result.getId());
        assertTrue(result.isPublished());
    }

    @Test
    public void testSendingUpdateNetworkMessage_shouldSucceed() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

        LocalisedStringEntity originalName = new LocalisedStringEntity("testiverkostonimi", null, null);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("TESTIVERKOSTO", originalName, null, null, true);

        List<NetworkOrganisationEntity> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            NetworkOrganisationEntity networkOrg = new NetworkOrganisationEntity();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrganisations.add(networkOrg);
        }

        networkEntity.setOrganisations(networkOrganisations);

        networkRepository.create(networkEntity);

        String updateJson =
            """
                {
                    "network": {
                        "id": "TESTIVERKOSTO",
                        "name": {
                            "values": {
                                "fi": "Uusi verkosto nimi",
                                "en": "Network name 2",
                                "sv": "Network namn 2"
                            }
                        }
                    }
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        Message m1 = jmsTemplate.receive("org_network_queue_1");
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_UPDATED_NOTIFICATION.name());

        NetworkUpdatedNotification resp2 = (NetworkUpdatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");
        assertEquals(resp2.getNetwork().getName().getValue("fi"), "Uusi verkosto nimi");
        assertEquals(resp2.getNetwork().getName().getValue("en"), "Network name 2");
        assertEquals(resp2.getNetwork().getName().getValue("sv"), "Network namn 2");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), MessageType.NETWORK_UPDATED_NOTIFICATION.name());
        
        NetworkUpdatedNotification resp3 = (NetworkUpdatedNotification) jmsTemplate.getMessageConverter().fromMessage(m2);
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
    public void testSendingUpdateNetworkMessage_organisationRemoved_shouldNotifyOriginalAndNewOrganisations() throws JMSException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2", "ORG3");
        createOrganisations(organisationIds, this.messageSchemaVersion);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("TESTIVERKOSTO", new LocalisedStringEntity("testiverkostonimi", null, null),
            Arrays.asList(
                new NetworkOrganisationEntity("ORG1", true, null),
                new NetworkOrganisationEntity("ORG2", false, null)),
            null, true);
        networkRepository.create(networkEntity);

        String updateJson =
            """
                {
                    "network": {
                        "id": "TESTIVERKOSTO",
                        "name": {
                            "values": {
                                "fi": "Uusi verkosto nimi"
                            }
                        },
                        "organisations": [
                            {
                                "organisationTkCode": "ORG1",
                                "isCoordinator": true,
                                "validityInNetwork": {
                                    "continuity": "INDEFINITELY",
                                    "start": "2019-05-03T15:33:52.587+03:00",
                                    "end": null
                                }
                            },
                            {
                                "organisationTkCode": "ORG3",
                                "isCoordinator": false,
                                "validityInNetwork": {
                                    "continuity": "INDEFINITELY",
                                    "start": "2019-05-03T15:33:52.587+03:00",
                                    "end": null
                                }
                            }
                        ]
                    }
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());

        NetworkEntity updatedNetwork = networkRepository.findById("TESTIVERKOSTO").orElse(null);
        assertNotNull(updatedNetwork);
        assertEquals(2, updatedNetwork.getOrganisations().size());

        List<String> updatedOrganisationIds = updatedNetwork.getOrganisations().stream()
            .map(NetworkOrganisationEntity::getOrganisationTkCode)
            .toList();
        assertTrue(updatedOrganisationIds.contains("ORG1"));
        assertTrue(updatedOrganisationIds.contains("ORG3"));
        assertFalse(updatedOrganisationIds.contains("ORG2"));

        Message org1Message = jmsTemplate.receive("org_network_queue_1");
        assertNotNull(org1Message);
        assertEquals(MessageType.NETWORK_UPDATED_NOTIFICATION.name(), org1Message.getStringProperty("messageType"));

        Message removedOrganisationMessage = jmsTemplate.receive("org_network_queue_2");
        assertNotNull(removedOrganisationMessage);
        assertEquals(MessageType.NETWORK_UPDATED_NOTIFICATION.name(), removedOrganisationMessage.getStringProperty("messageType"));

        NetworkUpdatedNotification removedOrganisationNotification =
            (NetworkUpdatedNotification) jmsTemplate.getMessageConverter().fromMessage(removedOrganisationMessage);
        assertEquals("TESTIVERKOSTO", removedOrganisationNotification.getNetwork().getId());

        List<String> notificationOrganisationIds = removedOrganisationNotification.getNetwork().getOrganisations().stream()
            .map(NetworkOrganisation::getOrganisationTkCode)
            .toList();
        assertTrue(notificationOrganisationIds.contains("ORG1"));
        assertTrue(notificationOrganisationIds.contains("ORG3"));
        assertFalse(notificationOrganisationIds.contains("ORG2"));

        Message addedOrganisationMessage = jmsTemplate.receive("org_network_queue_3");
        assertNotNull(addedOrganisationMessage);
        assertEquals(MessageType.NETWORK_UPDATED_NOTIFICATION.name(), addedOrganisationMessage.getStringProperty("messageType"));
    }

    @Test
    public void testSendingUpdateNetworkMessage_networkCreatedNotificationShouldBeSentWhenPublishing_shouldSucceed() throws JMSException, IOException {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2");
        createOrganisations(organisationIds, this.messageSchemaVersion);

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
        network.setPublished(false);

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

        String updateJson =
            """
                {
                    "network": {
                        "id": "TESTIVERKOSTO",
                        "name": {
                            "values": {
                                "fi": "Uusi verkosto nimi",
                                "en": "Network name 2",
                                "sv": "Network namn 2"
                            }
                        },
                        "published": true
                    }
                }
            """;

        responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        Message m1 = jmsTemplate.receive("org_network_queue_1");
        assertEquals(m1.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());

        NetworkCreatedNotification resp2 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m1);
        assertEquals(resp2.getNetwork().getId(), "TESTIVERKOSTO");
        assertEquals(resp2.getNetwork().getName().getValue("fi"), "Uusi verkosto nimi");
        assertEquals(resp2.getNetwork().getName().getValue("en"), "Network name 2");
        assertEquals(resp2.getNetwork().getName().getValue("sv"), "Network namn 2");

        Message m2 = jmsTemplate.receive("org_network_queue_2");
        assertEquals(m2.getStringProperty("messageType"), MessageType.NETWORK_CREATED_NOTIFICATION.name());
        
        NetworkCreatedNotification resp3 = (NetworkCreatedNotification) jmsTemplate.getMessageConverter().fromMessage(m2);
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
        assertTrue(result.isPublished());
        assertNotNull(result.getOrganisations());
        assertEquals(result.getOrganisations().size(), 2);
    }

    @Test
    public void testSendingCreateNetwork_shouldFailMissingRequiredFields() throws JMSException {
        String createJson =
        """
            {
            	"network": {
            		"id": "TESTIVERKOSTO",
            		"name": {
            			"values": {
            				"fi": "Uusi verkosto nimi",
            				"en": "Network name 2",
            				"sv": "Network namn 2"
            		    }
            		}
            	}
            }
        """;

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
        """
            {
            	"network": {
            		"name": {
            			"values": {
            				"fi": "Uusi verkosto nimi",
            				"en": "Network name 2",
            				"sv": "Network namn 2"
            		  }
            		}
            	}
            }
        """;

        Message responseMessage = JmsHelper.sendAndReceiveJsonWithUserId(jmsTemplate, updateJson, MessageType.UPDATE_NETWORK_REQUEST.name(), adminUiUser);
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);
        assertEquals(1, response.getErrors().size());
    }

    private void createOrganisations(List<String> organisationIds, int messageSchemaVersion) {
        for(int i = 1; i <= organisationIds.size(); i++) {
            OrganisationEntity orgEnt = EntityInitializer.getOrganisationEntity(organisationIds.get(i - 1), "org_network_queue_"+i, null, messageSchemaVersion);
            organisationRepository.create(orgEnt);
        }
    }

    private final String createNetworkJson =
        """
            {
                "network": {
                    "id": "CN-1",
                    "name": {
                        "values": {
                            "fi": "Verkosto 1",
                            "en": "Network 1",
                            "sv": null
                        }
                    },
                    "published": true,
                    "abbreviation": "V1",
                    "networkType": "CURRICULUM_LEVEL",
                    "organisations": [
                        {
                            "organisationTkCode": "UEF",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.587+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "HAAGAH",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.589+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "JUY",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.590+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "LAUREA",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.590+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "METROP",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.590+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "SAV",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.590+03:00",
                                "end": null
                            }
                        },
                        {
                            "organisationTkCode": "TUNI",
                            "isCoordinator": true,
                            "validityInNetwork": {
                                "continuity": "INDEFINITELY",
                                "start": "2019-05-03T15:33:52.590+03:00",
                                "end": null
                            }
                        }
                    ],
                    "validity": {
                        "continuity": "INDEFINITELY",
                        "start": "2019-01-01T00:00:00.000+03:00",
                        "end": null
                    },
                    "description": {
                        "values": {
                            "fi": "Verkosto, joka sisÃ¤ltÃ¤Ã¤ kaikki organisaatiot. Kaikki organisaatiot ovat organisoijia.",
                            "en": "Network that contains all organisations. All organisations are coordinators.",
                            "sv": null
                        }
                    }
                }
            }
        """;
}
