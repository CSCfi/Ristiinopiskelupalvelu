package fi.uta.ristiinopiskelu.handler.integration.route.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.HistoryHelper;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
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
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class UpdateRealisationRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRealisationRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedStringEntity(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(organisation);
            }
        }
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldSuccessHasForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU/1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("REAL/11", "REALCODE/11", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);

        realisationRepository.create(realisationEntity);

        String updateMessage =
        """
            {
                "realisation": {
                    "realisationId": "REAL/11",
                    "minSeats": 20,
                    "maxSeats": 200,
                    "status": "ARCHIVED",
                    "studyElementReferences": [
                      {
                           "referenceIdentifier": "CU/1",
                           "referenceOrganizer": "TUNI",
                           "referenceType": "COURSE_UNIT"
                      }
                   ],
                    "minEduGuidanceArea": [
                       5
                   ]
               }
            }
        """;
        Message updResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateMessage, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        DefaultResponse updresp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updResponseMessage);
        assertTrue(updresp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId(
            realisationEntity.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(realisationEntity.getRealisationId(), updatedEntity.getRealisationId());
        assertEquals(realisationEntity.getRealisationIdentifierCode(), updatedEntity.getRealisationIdentifierCode());
        assertEquals(updatedEntity.getStudyElementReferences().size(), 1);
        assertTrue(updatedEntity.getStudyElementReferences().stream().anyMatch(r -> r.getReferenceIdentifier().equals(courseUnitEntity.getStudyElementId())));
        assertEquals(20, updatedEntity.getMinSeats());
        assertEquals(200, updatedEntity.getMaxSeats());
        assertEquals(StudyStatusEntity.ARCHIVED, updatedEntity.getStatus());
        assertNotNull(updatedEntity.getMinEduGuidanceArea());
        assertEquals(1, updatedEntity.getMinEduGuidanceArea().size());
        assertEquals(MinEduGuidanceArea.BUSINESS_ADMINIST_AND_LAW.name(), updatedEntity.getMinEduGuidanceArea().get(0).name());
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId,
            Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
            "129177", organisingOrganisationId).orElse(null);

        assertNotNull(createdEntity);
        assertNotNull(createdEntity.getCreatedTime());

        assertNotEquals(createdEntity.getId(), "129177");
        assertNotEquals(createdEntity.getId(), "129177");
        assertEquals("129177", createdEntity.getRealisationId());
        assertEquals("129177", createdEntity.getRealisationIdentifierCode());
        assertEquals(10, createdEntity.getMinSeats());
        assertEquals(400, createdEntity.getMaxSeats());
        assertEquals(createdEntity.getStudyElementReferences().size(), 1);
        assertEquals(2, createdEntity.getPersonReferences().size());
        assertTrue(createdEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));

        // Send update message for realisation to remove one teacher (identifier 7228) and update min seats to 20 and max seats to 200
        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
            .orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity.getCreatedTime());
        assertNotEquals(updatedEntity.getId(), "129177");
        assertEquals("129177", updatedEntity.getRealisationId());
        assertEquals("129177", updatedEntity.getRealisationIdentifierCode());
        assertEquals(20, updatedEntity.getMinSeats());
        assertEquals(200, updatedEntity.getMaxSeats());
        assertEquals(1, updatedEntity.getPersonReferences().size());
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
    }

    @Test
    public void testSendingUpdateRealisationMessage_withExpiredCooperationNetwork_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";

        ValidityEntity validity = new ValidityEntity(ValidityEntity.ContinuityEntity.FIXED, OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusDays(2));
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Collections.singletonList(new NetworkOrganisationEntity(organisingOrganisationId, true, validity)),
            validity, true);
        networkRepository.create(networkEntity);

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().minusDays(2));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId,
            Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
            "129177", organisingOrganisationId).orElse(null);

        assertNotNull(createdEntity);
        assertNotNull(createdEntity.getCreatedTime());

        assertNotEquals(createdEntity.getId(), "129177");
        assertNotEquals(createdEntity.getId(), "129177");
        assertEquals("129177", createdEntity.getRealisationId());
        assertEquals("129177", createdEntity.getRealisationIdentifierCode());
        assertEquals(10, createdEntity.getMinSeats());
        assertEquals(400, createdEntity.getMaxSeats());
        assertEquals(createdEntity.getStudyElementReferences().size(), 1);
        assertEquals(2, createdEntity.getPersonReferences().size());
        assertTrue(createdEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));

        String updateRealisationJson = """
            {
              "realisation": {
                "realisationId": "129177",
                "minSeats": 20,
                "maxSeats": 200,
                "personReferences": [
                  {
                    "personRole": {
                      "key": "teacher",
                      "codeSetKey": "personRole"
                    },
                    "person": {
                      "homeEppn": "testi2@test.fi",
                      "surName": "Testinen2"
                    }
                  }
                ],
                "cooperationNetworks": [
                  {
                    "id": "%s",
                    "name": {
                      "values": {
                        "fi": "Verkosto",
                        "en": "Verkosto en",
                        "sv": "Verkosto sv"
                      }
                    },
                    "enrollable": true
                  }
                ]
              }
            }
            """.formatted(networkEntity.getId());

        // Send update message for realisation to remove one teacher (identifier 7228) and update min seats to 20 and max seats to 200
        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
            .orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity.getCreatedTime());
        assertNotEquals(updatedEntity.getId(), "129177");
        assertEquals("129177", updatedEntity.getRealisationId());
        assertEquals("129177", updatedEntity.getRealisationIdentifierCode());
        assertEquals(20, updatedEntity.getMinSeats());
        assertEquals(200, updatedEntity.getMaxSeats());
        assertEquals(1, updatedEntity.getPersonReferences().size());
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
    }

    @Test
    public void testSendingUpdateRealisationMessage_networksChanged_shouldNotifyOriginalAndNewNetworkMembers() throws JMSException {
        String organisingOrganisationId = "TUNI";
        String removedOrganisationId = "REALISATION-REMOVED-ORG";
        String addedOrganisationId = "REALISATION-ADDED-ORG";

        OrganisationEntity removedOrganisation = EntityInitializer.getOrganisationEntity(removedOrganisationId, removedOrganisationId,
            new LocalisedStringEntity(removedOrganisationId, null, null), messageSchemaVersion);
        removedOrganisation.setNotificationsEnabled(true);
        organisationService.create(removedOrganisation);

        OrganisationEntity addedOrganisation = EntityInitializer.getOrganisationEntity(addedOrganisationId, addedOrganisationId,
            new LocalisedStringEntity(addedOrganisationId, null, null), messageSchemaVersion);
        addedOrganisation.setNotificationsEnabled(true);
        organisationService.create(addedOrganisation);

        NetworkEntity originalNetwork = persistNetworkEntity("CN-REALISATION-ORIGINAL",
            new LocalisedStringEntity("Original network", null, null), Arrays.asList(organisingOrganisationId, removedOrganisationId));
        NetworkEntity updatedNetwork = persistNetworkEntity("CN-REALISATION-UPDATED",
            new LocalisedStringEntity("Updated network", null, null), Arrays.asList(organisingOrganisationId, addedOrganisationId));

        CooperationNetworkEntity originalCooperationNetwork = EntityInitializer.getCooperationNetwork(originalNetwork.getId(), originalNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        CooperationNetworkEntity updatedCooperationNetwork = EntityInitializer.getCooperationNetwork(updatedNetwork.getId(), updatedNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-REALISATION-NOTIFICATIONS", "CU-REALISATION-NOTIFICATIONS",
            organisingOrganisationId, Arrays.asList(originalCooperationNetwork, updatedCooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity studyElementReference = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("REALISATION-NOTIFICATIONS", "REALISATION-NOTIFICATIONS",
            organisingOrganisationId, Collections.singletonList(studyElementReference), Collections.singletonList(originalCooperationNetwork));
        realisationRepository.create(realisationEntity);

        String updateJson = """
            {
              "realisation": {
                "realisationId": "REALISATION-NOTIFICATIONS",
                "cooperationNetworks": [
                  {
                    "id": "CN-REALISATION-UPDATED"
                  }
                ]
              }
            }
            """;

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson,
            MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertEquals(Status.OK, updateResp.getStatus());

        RealisationEntity updatedRealisation = realisationRepository.findByRealisationIdAndOrganizingOrganisationId(
            realisationEntity.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(updatedRealisation);
        assertEquals(1, updatedRealisation.getCooperationNetworks().size());
        assertEquals(updatedNetwork.getId(), updatedRealisation.getCooperationNetworks().getFirst().getId());

        Message removedOrganisationMessage = JmsHelper.receiveObject(jmsTemplate, removedOrganisationId);
        assertNotNull(removedOrganisationMessage);
        CompositeIdentifiedEntityModifiedNotification removedOrganisationNotification =
            (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(removedOrganisationMessage);
        assertEquals(1, removedOrganisationNotification.getDeleted().size());
        assertTrue(removedOrganisationNotification.getCreated().isEmpty());
        assertTrue(removedOrganisationNotification.getUpdated().isEmpty());
        assertEquals(realisationEntity.getRealisationId(), removedOrganisationNotification.getDeleted().getFirst().getReferenceIdentifier());
        assertEquals(organisingOrganisationId, removedOrganisationNotification.getDeleted().getFirst().getReferenceOrganizer());
        assertEquals(CompositeIdentifiedEntityType.REALISATION, removedOrganisationNotification.getDeleted().getFirst().getType());

        Message addedOrganisationMessage = JmsHelper.receiveObject(jmsTemplate, addedOrganisationId);
        assertNotNull(addedOrganisationMessage);
        CompositeIdentifiedEntityModifiedNotification addedOrganisationNotification =
            (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(addedOrganisationMessage);
        assertEquals(1, addedOrganisationNotification.getCreated().size());
        assertTrue(addedOrganisationNotification.getUpdated().isEmpty());
        assertTrue(addedOrganisationNotification.getDeleted().isEmpty());
        assertEquals(realisationEntity.getRealisationId(), addedOrganisationNotification.getCreated().getFirst().getReferenceIdentifier());
        assertEquals(organisingOrganisationId, addedOrganisationNotification.getCreated().getFirst().getReferenceOrganizer());
        assertEquals(CompositeIdentifiedEntityType.REALISATION, addedOrganisationNotification.getCreated().getFirst().getType());
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldFailMissingRequiredFields() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";
        String updateJson =
        """
            {
                "realisation": {
                    "minSeats": 20
               }
            }
        """;
        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);
        assertEquals(1, response.getErrors().size());
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldNotFailBecauseOfExtraField() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        String updateJson =
        """
            {
                "realisation": {
                    "realisationId": "129177",
                    "minSeats": 20,
                    "maxSeats": 200,
                    "thisdowsnotexist": "asdsdasdsad"
               }
            }
        """;
        Message updateResponse = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(updateResponse);

        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.OK);
    }

    @Test
    public void testSendingUpdateRealisationMessageWithEmptyCooperationNetworks_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        String updateJson =
        """
            {
                "realisation": {
                    "realisationId": "129177",
                    "minSeats": 20,
                    "maxSeats": 200,
                    "cooperationNetworks": [
                    ]
               }
            }
        """;
        Message updateResponse = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(updateResponse);

        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId,
            Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
            "129177", organisingOrganisationId).orElse(null);

        // Send update message for realisation to remove one teacher (identifier 7228) and update min seats to 20 and max seats to 200
        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
            .orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity.getCreatedTime());
        assertEquals(createdEntity.getId(), updatedEntity.getId());
        assertNotEquals(createdEntity.getId(), "129177");
        assertNotEquals(updatedEntity.getId(), "129177");
        assertEquals("129177", createdEntity.getRealisationId());
        assertEquals("129177", createdEntity.getRealisationIdentifierCode());
        assertEquals("129177", updatedEntity.getRealisationId());
        assertEquals("129177", updatedEntity.getRealisationIdentifierCode());
        assertEquals(20, updatedEntity.getMinSeats());
        assertEquals(200, updatedEntity.getMaxSeats());
        assertEquals(1, updatedEntity.getPersonReferences().size());
        assertTrue(updatedEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertTrue(updatedEntity.getPersonReferences().stream().noneMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));

        List<RealisationEntity> historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate, "toteutukset-history", RealisationEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        RealisationEntity historyEntity = historyEntities.get(0);
        assertNotNull(historyEntity.getCreatedTime());
        assertEquals("129177", historyEntity.getRealisationId());
        assertEquals(10, historyEntity.getMinSeats());
        assertEquals(400, historyEntity.getMaxSeats());
        assertEquals(historyEntity.getStudyElementReferences().size(), 1);
        assertEquals(2, historyEntity.getPersonReferences().size());
        assertTrue(historyEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));

        // Send update message for realisation to change one teacher and update min seats to 40 and max seats to 400
        updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationJson2, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // verify update
        RealisationEntity updatedEntity2 = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
            .orElse(null);
        assertNotNull(updatedEntity2);
        assertNotNull(updatedEntity2.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity2.getCreatedTime());
        assertNotEquals(updatedEntity2.getId(), "129177");
        assertEquals("129177", updatedEntity2.getRealisationId());
        assertEquals("129177", updatedEntity2.getRealisationIdentifierCode());
        assertEquals(40, updatedEntity2.getMinSeats());
        assertEquals(400, updatedEntity2.getMaxSeats());
        assertEquals(1, updatedEntity2.getPersonReferences().size());
        assertTrue(updatedEntity2.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi3@test.fi")));

        //  verify new history document (data should be like data was after first update)
        historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate, "toteutukset-history", RealisationEntity.class);
        assertNotNull(historyEntities);
        assertEquals(2, historyEntities.size());

        RealisationEntity newHistoryEntity = historyEntities.stream()
            .sorted(Comparator.comparing(RealisationEntity::getUpdateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
            .findFirst()
            .orElse(null);

        assertNotNull(newHistoryEntity);
        assertEquals(updatedEntity.getUpdateTime(), newHistoryEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), newHistoryEntity.getCreatedTime());
        assertEquals(20, newHistoryEntity.getMinSeats());
        assertEquals(200, newHistoryEntity.getMaxSeats());
        assertEquals(1, newHistoryEntity.getPersonReferences().size());
        assertTrue(newHistoryEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertTrue(newHistoryEntity.getPersonReferences().stream().noneMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldUpdateStatus() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
            "129177", organisingOrganisationId).orElse(null);
        assertNotNull(createdEntity);
        assertEquals(StudyStatusEntity.ACTIVE, createdEntity.getStatus());

        // Send update message for realisation to remove one teacher (identifier 7228) and update min seats to 20 and max seats to 200
        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationStatusJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
            .orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity.getCreatedTime());
        assertEquals(StudyStatusEntity.CANCELLED, updatedEntity.getStatus());
    }

    @Test
    public void testUpdateRealisationMessage_updatesCourseUnitReferences_shouldAddRemoveAndUpdateDenormalizedDataCorrectly() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntity.setName(new LocalisedStringEntity("Nimi 1", "Name 1", "Namn 1"));
        realisationEntity.setStartDate(LocalDate.now().minusMonths(1));
        realisationEntity.setEndDate(LocalDate.now().plusMonths(6));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        CourseUnitEntity courseUnitToAddDenormalizedData = EntityInitializer.getCourseUnitEntity(
            "CU1", "CODE1", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitToAddDenormalizedData);

        CourseUnitEntity courseUnitToRemoveDenormalizedDataFrom = EntityInitializer.getCourseUnitEntity(
            "CU2", "CODE2", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitToRemoveDenormalizedDataFrom);
        courseUnitToRemoveDenormalizedDataFrom.setRealisations(Collections.singletonList(CourseUnitRealisationEntity.from(realisationEntity)));

        CourseUnitEntity courseUnitToUpdateDenormalizedData = EntityInitializer.getCourseUnitEntity(
            "CU3", "CODE3", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitToUpdateDenormalizedData.setRealisations(Collections.singletonList(CourseUnitRealisationEntity.from(realisationEntity)));

        courseUnitRepository.create(courseUnitToUpdateDenormalizedData);

        StudyElementReferenceEntity refToRemove = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitToRemoveDenormalizedDataFrom.getStudyElementId(), organisingOrganisationId);

        StudyElementReferenceEntity refToUpdate = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitToUpdateDenormalizedData.getStudyElementId(), organisingOrganisationId);

        realisationEntity.setStudyElementReferences(Arrays.asList(refToRemove, refToUpdate));

        realisationRepository.create(realisationEntity);

        String updateMessage =
        """
            {
                "realisation": {
                    "realisationId": "%s",
                    "startDate": "2019-11-09",
                    "endDate": "2020-07-09",
                    "enrollmentStartDateTime": "2020-02-01T00:00:00.000+02:00",
                    "enrollmentEndDateTime": "2020-02-10T00:00:00.000+02:00",
                    "name": {
                        "values": {
                            "fi": "Nimi 2",
                            "sv": "Namn 2",
                            "en": "Name 2"
                        }
                    },
                    "realisationIdentifierCode": "NEWCODE1",
                    "studyElementReferences": [
                        {
                            "referenceIdentifier": "%s",
                            "referenceOrganizer": "%s",
                            "referenceType": "COURSE_UNIT"
                        },
                        {
                            "referenceIdentifier": "%s",
                            "referenceOrganizer": "%s",
                            "referenceType": "COURSE_UNIT"
                        }
                    ]
                }
            }
        """.formatted(realisationEntity.getRealisationId(), courseUnitToUpdateDenormalizedData.getStudyElementId(), courseUnitToUpdateDenormalizedData.getOrganizingOrganisationId(), courseUnitToAddDenormalizedData.getStudyElementId(), courseUnitToAddDenormalizedData.getOrganizingOrganisationId());

        Message updResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateMessage, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        DefaultResponse updresp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updResponseMessage);
        assertTrue(updresp.getStatus() == Status.OK);

        RealisationEntity updatedRealisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
            realisationEntity.getRealisationId(), organisingOrganisationId).orElse(null);

        // Verify denormalized data is added
        courseUnitToAddDenormalizedData = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToAddDenormalizedData.getStudyElementId(), courseUnitToAddDenormalizedData.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToAddDenormalizedData);
        assertEquals(1, courseUnitToAddDenormalizedData.getRealisations().size());

        CourseUnitRealisationEntity denormalizedRealisation = courseUnitToAddDenormalizedData.getRealisations().get(0);
        assertEquals(updatedRealisationEntity.getRealisationId(), denormalizedRealisation.getRealisationId());
        assertEquals(updatedRealisationEntity.getRealisationIdentifierCode(), denormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(updatedRealisationEntity.getName().getValue("fi"), denormalizedRealisation.getName().getValue("fi"));
        assertEquals(updatedRealisationEntity.getName().getValue("en"), denormalizedRealisation.getName().getValue("en"));
        assertEquals(updatedRealisationEntity.getName().getValue("sv"), denormalizedRealisation.getName().getValue("sv"));
        assertEquals(updatedRealisationEntity.getEnrollmentStartDateTime(), denormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(updatedRealisationEntity.getEnrollmentEndDateTime(), denormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(updatedRealisationEntity.getStartDate(), denormalizedRealisation.getStartDate());
        assertEquals(updatedRealisationEntity.getEndDate(), denormalizedRealisation.getEndDate());

        // Verify denormalized data is updated
        courseUnitToUpdateDenormalizedData = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToUpdateDenormalizedData.getStudyElementId(), courseUnitToUpdateDenormalizedData.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToUpdateDenormalizedData);
        assertEquals(1, courseUnitToUpdateDenormalizedData.getRealisations().size());

        CourseUnitRealisationEntity updatedDenormalizedRealisation = courseUnitToUpdateDenormalizedData.getRealisations().get(0);
        assertEquals(updatedRealisationEntity.getRealisationId(), updatedDenormalizedRealisation.getRealisationId());
        assertEquals(updatedRealisationEntity.getRealisationIdentifierCode(), updatedDenormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(updatedRealisationEntity.getName().getValue("fi"), updatedDenormalizedRealisation.getName().getValue("fi"));
        assertEquals(updatedRealisationEntity.getName().getValue("en"), updatedDenormalizedRealisation.getName().getValue("en"));
        assertEquals(updatedRealisationEntity.getName().getValue("sv"), updatedDenormalizedRealisation.getName().getValue("sv"));
        assertEquals(updatedRealisationEntity.getEnrollmentStartDateTime(), updatedDenormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(updatedRealisationEntity.getEnrollmentEndDateTime(), updatedDenormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(updatedRealisationEntity.getStartDate(), updatedDenormalizedRealisation.getStartDate());
        assertEquals(updatedRealisationEntity.getEndDate(), updatedDenormalizedRealisation.getEndDate());

        // Verify denormalized data is removed
        courseUnitToRemoveDenormalizedDataFrom = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToRemoveDenormalizedDataFrom.getStudyElementId(), courseUnitToRemoveDenormalizedDataFrom.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToRemoveDenormalizedDataFrom);
        assertNull(courseUnitToRemoveDenormalizedDataFrom.getRealisations());
    }

    @Test
    public void testUpdateRealisationMessage_updatesAssessmentItemReferences_shouldAddRemoveAndUpdateDenormalizedDataCorrectly() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntity.setName(new LocalisedStringEntity("Nimi 1", "Name 1", "Namn 1"));
        realisationEntity.setStartDate(LocalDate.now().minusMonths(1));
        realisationEntity.setEndDate(LocalDate.now().plusMonths(6));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO1",
            Arrays.asList(EntityInitializer.getAssessmentItemEntity("AI1", "CU1"),
                EntityInitializer.getAssessmentItemEntity("AI2", "CU1")));

        CourseUnitEntity courseUnitToAddDenormalizedData = EntityInitializer.getCourseUnitEntity(
            "CU1", "CODE1", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitToAddDenormalizedData.setCompletionOptions(Collections.singletonList(completionOptionEntity));
        courseUnitRepository.create(courseUnitToAddDenormalizedData);

        CourseUnitRealisationEntity courseUnitRealisationEntity = CourseUnitRealisationEntity.from(realisationEntity);
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI3", "CU1");
        assessmentItemEntity.setRealisations(Collections.singletonList(courseUnitRealisationEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO2",
            Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitToRemoveDenormalizedDataFrom = EntityInitializer.getCourseUnitEntity(
            "CU2", "CODE2", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitToRemoveDenormalizedDataFrom.setCompletionOptions(Collections.singletonList(completionOptionEntity2));
        courseUnitRepository.create(courseUnitToRemoveDenormalizedDataFrom);

        CourseUnitRealisationEntity courseUnitRealisationEntity2 = CourseUnitRealisationEntity.from(realisationEntity);
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI3", "CU1");
        assessmentItemEntity.setRealisations(Collections.singletonList(courseUnitRealisationEntity2));
        CompletionOptionEntity completionOptionEntity3 = EntityInitializer.getCompletionOptionEntity("CO3",
            Collections.singletonList(assessmentItemEntity2));

        CourseUnitEntity courseUnitToUpdateDenormalizedData = EntityInitializer.getCourseUnitEntity(
            "CU3", "CODE3", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitToUpdateDenormalizedData.setCompletionOptions(Collections.singletonList(completionOptionEntity3));
        courseUnitRepository.create(courseUnitToUpdateDenormalizedData);

        StudyElementReferenceEntity refToRemove = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitToRemoveDenormalizedDataFrom.getStudyElementId(), organisingOrganisationId, assessmentItemEntity.getAssessmentItemId());

        StudyElementReferenceEntity refToUpdate = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitToUpdateDenormalizedData.getStudyElementId(), organisingOrganisationId, assessmentItemEntity2.getAssessmentItemId());

        realisationEntity.setStudyElementReferences(Arrays.asList(refToRemove, refToUpdate));

        realisationRepository.create(realisationEntity);

        String updateMessage =
        """
            {
                "realisation": {
                    "realisationId": "%s",
                    "startDate": "2019-11-09",
                    "endDate": "2020-07-09",
                    "enrollmentStartDateTime": "2020-02-01T00:00:00.000+02:00",
                    "enrollmentEndDateTime": "2020-02-10T00:00:00.000+02:00",
                    "name": {
                        "values": {
                            "fi": "Nimi 2",
                            "sv": "Namn 2",
                            "en": "Name 2"
                        }
                    },
                    "realisationIdentifierCode": "NEWCODE1",
                    "studyElementReferences": [
                        {
                            "referenceIdentifier": "%s",
                            "referenceOrganizer": "%s",
                            "referenceAssessmentItemId": "%s",
                            "referenceType": "ASSESSMENT_ITEM"
                        },
                        {
                            "referenceIdentifier": "%s",
                            "referenceOrganizer": "%s",
                            "referenceAssessmentItemId": "AI1",
                            "referenceType": "ASSESSMENT_ITEM"
                        }
                    ]
                }
            }
        """.formatted(realisationEntity.getRealisationId(), courseUnitToUpdateDenormalizedData.getStudyElementId(), courseUnitToUpdateDenormalizedData.getOrganizingOrganisationId(), assessmentItemEntity2.getAssessmentItemId(), courseUnitToAddDenormalizedData.getStudyElementId(), courseUnitToAddDenormalizedData.getOrganizingOrganisationId());

        Message updResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateMessage, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        DefaultResponse updresp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updResponseMessage);
        assertTrue(updresp.getStatus() == Status.OK);

        RealisationEntity updatedRealisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
            realisationEntity.getRealisationId(), organisingOrganisationId).orElse(null);

        // Verify denormalized data is added
        courseUnitToAddDenormalizedData = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToAddDenormalizedData.getStudyElementId(), courseUnitToAddDenormalizedData.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToAddDenormalizedData);
        assertEquals(1, courseUnitToAddDenormalizedData.getAssessmentItemRealisations().size());

        CourseUnitRealisationEntity denormalizedRealisation = courseUnitToAddDenormalizedData.getAssessmentItemRealisations().get(0);
        assertEquals(updatedRealisationEntity.getRealisationId(), denormalizedRealisation.getRealisationId());
        assertEquals(updatedRealisationEntity.getRealisationIdentifierCode(), denormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(updatedRealisationEntity.getName().getValue("fi"), denormalizedRealisation.getName().getValue("fi"));
        assertEquals(updatedRealisationEntity.getName().getValue("en"), denormalizedRealisation.getName().getValue("en"));
        assertEquals(updatedRealisationEntity.getName().getValue("sv"), denormalizedRealisation.getName().getValue("sv"));
        assertEquals(updatedRealisationEntity.getEnrollmentStartDateTime(), denormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(updatedRealisationEntity.getEnrollmentEndDateTime(), denormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(updatedRealisationEntity.getStartDate(), denormalizedRealisation.getStartDate());
        assertEquals(updatedRealisationEntity.getEndDate(), denormalizedRealisation.getEndDate());

        // Verify denormalized data is updated
        courseUnitToUpdateDenormalizedData = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToUpdateDenormalizedData.getStudyElementId(), courseUnitToUpdateDenormalizedData.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToUpdateDenormalizedData);
        assertEquals(1, courseUnitToAddDenormalizedData.getAssessmentItemRealisations().size());

        CourseUnitRealisationEntity updatedDenormalizedRealisation = courseUnitToAddDenormalizedData.getAssessmentItemRealisations().get(0);
        assertEquals(updatedRealisationEntity.getRealisationId(), updatedDenormalizedRealisation.getRealisationId());
        assertEquals(updatedRealisationEntity.getRealisationIdentifierCode(), updatedDenormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(updatedRealisationEntity.getName().getValue("fi"), updatedDenormalizedRealisation.getName().getValue("fi"));
        assertEquals(updatedRealisationEntity.getName().getValue("en"), updatedDenormalizedRealisation.getName().getValue("en"));
        assertEquals(updatedRealisationEntity.getName().getValue("sv"), updatedDenormalizedRealisation.getName().getValue("sv"));
        assertEquals(updatedRealisationEntity.getEnrollmentStartDateTime(), updatedDenormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(updatedRealisationEntity.getEnrollmentEndDateTime(), updatedDenormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(updatedRealisationEntity.getStartDate(), updatedDenormalizedRealisation.getStartDate());
        assertEquals(updatedRealisationEntity.getEndDate(), updatedDenormalizedRealisation.getEndDate());

        // Verify denormalized data is removed
        courseUnitToRemoveDenormalizedDataFrom = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitToRemoveDenormalizedDataFrom.getStudyElementId(), courseUnitToRemoveDenormalizedDataFrom.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitToRemoveDenormalizedDataFrom);
        assertNull(courseUnitToRemoveDenormalizedDataFrom.getRealisations());
    }

    private final String testRealisationJson =
    """
        {
            "realisations": [
                {
                    "status": "ACTIVE",
                    "realisationId": "129177",
                    "realisationIdentifierCode": "129177",
                    "selections": null,
                    "questionSets": null,
                    "enrollmentStartDateTime": "2018-09-30T21:00:00.000Z",
                    "enrollmentEndDateTime": "2018-12-04T22:00:00.000Z",
                    "startDate": "2019-01-06",
                    "endDate": "2019-05-30",
                    "creditsMin": 5,
                    "creditsMax": 15,
                    "minSeats": 10,
                    "maxSeats": 400,
                    "name": {
                       "values": {
                            "fi": "nimi fi",
                            "sv": null,
                            "en": null
                        }
                     },
                    "personReferences": [
                        {
                            "personRole": {
                                "key": "teacher",
                                "codeSetKey": "personRole"
                            },
                            "person": {
                                "homeEppn": "testi@test.fi",
                                "surName": "Testinen"
                            }
                        },
                        {
                            "personRole": {
                                "key": "teacher",
                                "codeSetKey": "personRole"
                            },
                            "person": {
                                "homeEppn": "testi2@test.fi",
                                "surName": "Testinen"
                            }
                        }
                    ],
                    "studyElementReferences": [
                       {
                            "referenceIdentifier": "CU-1",
                            "referenceOrganizer": "TUNI",
                            "referenceType": "COURSE_UNIT"
                       }
                    ],
                   "organisationReferences": [
                       {
                           "organisationRole": 1,
                           "target": {
                               "organisationIdentifier": "TUNI",
                               "organisationTkCode": "TUNI"
                           }
                       }
                   ],
                   "cooperationNetworks": [
                       {
                           "id": "CN-1",
                           "name": {
                               "values": {
                                   "fi": "Verkosto 1",
                                   "en": "Network 1",
                                   "sv": null
                               }
                           },
                           "enrollable": true
                       }
                   ],
                   "teachingLanguage": [
                       "fi"
                   ]
                }
            ]
        }
    """;

    private final String updateRealisationJson =
    """
        {
            "realisation": {
                "realisationId": "129177",
                "minSeats": 20,
                "maxSeats": 200,
                "personReferences": [
                   {
                       "personRole": {
                           "key": "teacher",
                           "codeSetKey": "personRole"
                       },
                       "person": {
                           "homeEppn": "testi2@test.fi",
                            "surName": "Testinen2"
                       }
                   }
                ]
           }
        }
    """;

    private final String updateRealisationJson2 =
    """
        {
            "realisation": {
                "realisationId": "129177",
                "minSeats": 40,
                "maxSeats": 400,
                "personReferences": [
                   {
                       "personRole": {
                           "key": "teacher",
                           "codeSetKey": "personRole"
                       },
                       "person": {
                           "homeEppn": "testi3@test.fi",
                           "surName": "Testinen3"
                       }
                   }
                ]
           }
        }
    """;

    private final String updateRealisationStatusJson =
    """
        {
            "realisation": {
                "realisationId": "129177",
                "realisationIdentifierCode": "129177",
                "status": "CANCELLED"
           }
        }
    """;
}
