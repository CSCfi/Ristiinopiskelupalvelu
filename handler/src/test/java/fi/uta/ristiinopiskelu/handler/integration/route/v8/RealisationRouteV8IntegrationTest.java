package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
    EmbeddedActiveMQInitializer.class,
    EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class RealisationRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RealisationRouteV8IntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    private int messageSchemaVersion = 8;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                        new LocalisedStringEntity(organisationId, null, null),
                    this.messageSchemaVersion);
                organisationService.create(organisation);
            }
        }
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork =
            EntityInitializer.getCooperationNetwork(networkEntity.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdRealisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
                "129177", "TUNI").orElse(null);
        assertNotNull(createdRealisationEntity);
        assertNotNull(createdRealisationEntity.getCreatedTime());
        assertNotEquals(createdRealisationEntity.getId(), "129177");
        assertNotEquals(createdRealisationEntity.getId(), "129177");
        assertEquals("129177", createdRealisationEntity.getRealisationId());
        assertEquals("129177", createdRealisationEntity.getRealisationIdentifierCode());
        assertEquals(10, createdRealisationEntity.getMinSeats());
        assertEquals(400, createdRealisationEntity.getMaxSeats());
        assertEquals(createdRealisationEntity.getStudyElementReferences().size(), 1);
        assertEquals(2, createdRealisationEntity.getPersonReferences().size());
        assertTrue(createdRealisationEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(createdRealisationEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(createdRealisationEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE.name(), createdRealisationEntity.getStatus().name());
        assertEquals(BigDecimal.valueOf(5.0), createdRealisationEntity.getCreditsMin());
        assertEquals(BigDecimal.valueOf(15.0), createdRealisationEntity.getCreditsMax());

        // Verify cooperation network data was gathered from network index
        CooperationNetworkEntity realisationNetwork = createdRealisationEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), realisationNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), realisationNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), realisationNetwork.getName().getValue("sv"));

        // Verify created realisation data is denormalized to course unit
        CourseUnitEntity updatedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).orElse(null);
        assertEquals(1, updatedCourseUnitEntity.getRealisations().size());

        CourseUnitRealisationEntity denormalizedRealisation = updatedCourseUnitEntity.getRealisations().get(0);
        assertEquals(createdRealisationEntity.getRealisationId(), denormalizedRealisation.getRealisationId());
        assertEquals(createdRealisationEntity.getRealisationIdentifierCode(), denormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(createdRealisationEntity.getName().getValue("fi"), denormalizedRealisation.getName().getValue("fi"));
        assertEquals(createdRealisationEntity.getName().getValue("en"), denormalizedRealisation.getName().getValue("en"));
        assertEquals(createdRealisationEntity.getName().getValue("sv"), denormalizedRealisation.getName().getValue("sv"));
        assertEquals(createdRealisationEntity.getEnrollmentStartDateTime(), denormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(createdRealisationEntity.getEnrollmentEndDateTime(), denormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(createdRealisationEntity.getStartDate(), denormalizedRealisation.getStartDate());
        assertEquals(createdRealisationEntity.getEndDate(), denormalizedRealisation.getEndDate());
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldSuccessHasForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        NetworkEntity networkEntity2 = persistNetworkEntity("CN-2",
                new LocalisedStringEntity("Verkosto 2", "Verkosto 2 en", "Verkosto 2 sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetworkEntity cooperationNetwork2 = EntityInitializer.getCooperationNetwork(networkEntity2.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference orgRef = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU/1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        CooperationNetwork cooperationNetworkV8 = DtoInitializerV8.getCooperationNetwork(networkEntity.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork cooperationNetwork2V8 = DtoInitializerV8.getCooperationNetwork(networkEntity2.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Realisation realisation = DtoInitializerV8.getRealisation("REAL/11", "REALCODE/11", new LocalisedString("toteutus", null, null),
                Collections.singletonList(studyElementReference), Arrays.asList(cooperationNetworkV8, cooperationNetwork2V8), Collections.singletonList(orgRef));
        realisation.setStatus(StudyStatus.ARCHIVED);

        CreateRealisationRequest realisationRequest = new CreateRealisationRequest();
        realisationRequest.setRealisations(Collections.singletonList(realisation));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, realisationRequest, organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
                realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdEntity);
        assertNotNull(createdEntity.getCreatedTime());
        assertEquals(realisation.getRealisationId(), createdEntity.getRealisationId());
        assertEquals(realisation.getRealisationIdentifierCode(), createdEntity.getRealisationIdentifierCode());
        assertEquals(createdEntity.getStudyElementReferences().size(), 1);
        assertTrue(createdEntity.getStudyElementReferences().stream().anyMatch(r -> r.getReferenceIdentifier().equals(courseUnitEntity.getStudyElementId())));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED.name(), createdEntity.getStatus().name());

        // Verify cooperation network data was gathered from network index
        assertTrue(createdEntity.getCooperationNetworks().stream().anyMatch(cn ->
                cn.getName().getValue("fi").equals(networkEntity.getName().getValue("fi"))
                        && cn.getName().getValue("en").equals(networkEntity.getName().getValue("en"))
                        && cn.getName().getValue("sv").equals(networkEntity.getName().getValue("sv"))
                        ));

        assertTrue(createdEntity.getCooperationNetworks().stream().anyMatch(cn ->
                cn.getName().getValue("fi").equals(networkEntity2.getName().getValue("fi"))
                        && cn.getName().getValue("en").equals(networkEntity2.getName().getValue("en"))
                        && cn.getName().getValue("sv").equals(networkEntity2.getName().getValue("sv"))
                        ));
    }

    @Test
    public void testSendingUpdateRealisationMessage_shouldSuccessHasForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
                "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED.name(), updatedEntity.getStatus().name());
    }

    @Test
    public void testSendingUpdateRealisationMessage_teachingLanguageInAllCapsOrInvalid_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

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
                    "teachingLanguage": [
                       "SV"
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
        assertEquals("sv", updatedEntity.getTeachingLanguage().get(0));

        updateMessage =
        """
            {
                "realisation": {
                    "realisationId": "REAL/11",
                    "teachingLanguage": [
                       "VXCZVZCX"
                   ]
               }
            }
        """;
        updResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateMessage, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);
        updresp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updResponseMessage);
        assertTrue(updresp.getStatus() == Status.OK);

        updatedEntity = realisationService.findByIdAndOrganizingOrganisationId(
            realisationEntity.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(realisationEntity.getRealisationId(), updatedEntity.getRealisationId());
        assertEquals(realisationEntity.getRealisationIdentifierCode(), updatedEntity.getRealisationIdentifierCode());
        assertEquals(0, updatedEntity.getTeachingLanguage().size());
    }

    @Test
    public void testCreateRealisationMessageWithMultipleCUReals_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork),  null);
        courseUnitRepository.create(courseUnitEntity);

        CourseUnitEntity courseUnitEntity2 = EntityInitializer.getCourseUnitEntity(
                "CU-2", "4CO19KEK1000", organisingOrganisationId, Collections.singletonList(cooperationNetwork),  null);
        courseUnitRepository.create(courseUnitEntity2);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, twoRealisationsJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
                "129177", "TUNI").orElse(null);
        assertNotNull(createdEntity);
        assertNotNull(createdEntity.getCreatedTime());
        assertNotEquals(createdEntity.getId(), "129177");
        assertEquals("129177", createdEntity.getRealisationId());
        assertEquals(10, createdEntity.getMinSeats());
        assertEquals(400, createdEntity.getMaxSeats());
        assertEquals(createdEntity.getStudyElementReferences().size(), 1);
        assertEquals(2, createdEntity.getPersonReferences().size());
        assertTrue(createdEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE.name(), createdEntity.getStatus().name());

        RealisationEntity createdEntity2 = realisationService.findByIdAndOrganizingOrganisationId(
                "129178", "TUNI").orElse(null);
        assertNotNull(createdEntity2);
        assertNotNull(createdEntity2.getCreatedTime());
        assertNotEquals(createdEntity2.getId(), "129178");
        assertEquals("129178", createdEntity2.getRealisationId());
        assertEquals("129178", createdEntity2.getRealisationIdentifierCode());
        assertEquals(10, createdEntity2.getMinSeats());
        assertEquals(400, createdEntity2.getMaxSeats());
        assertEquals(1, createdEntity2.getStudyElementReferences().size());
        assertEquals(2, createdEntity2.getPersonReferences().size());
        assertTrue(createdEntity2.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(createdEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE.name(), createdEntity2.getStatus().name());
    }

    @Test
    public void testSendingCreateRealisationMessageWithAssessmentItemRef_shouldSuccess() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "CU-1");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("COO-1", Collections.singletonList(assessmentItemEntity));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), organisingOrganisationId, assessmentItemEntity.getAssessmentItemId());

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetworkV8 = DtoInitializerV8.getCooperationNetwork(networkEntity.getId(), 
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Realisation realisation = DtoInitializerV8.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
                Collections.singletonList(studyElementReference), Collections.singletonList(cooperationNetworkV8), Collections.singletonList(organisationReference));
        realisation.setStatus(StudyStatus.CANCELLED);

        CreateRealisationRequest req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        Message resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse response = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.OK);

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(
                realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation);
        assertNotNull(createdRealisation.getCreatedTime());
        assertNotEquals(createdRealisation.getId(), realisation.getRealisationId());
        assertNotEquals(createdRealisation.getId(), realisation.getRealisationIdentifierCode());
        assertEquals(realisation.getRealisationId(), createdRealisation.getRealisationId());
        assertEquals(realisation.getRealisationIdentifierCode(), createdRealisation.getRealisationIdentifierCode());
        assertEquals(realisation.getStudyElementReferences().size(), createdRealisation.getStudyElementReferences().size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.CANCELLED.name(), createdRealisation.getStatus().name());

        StudyElementReferenceEntity createdRealisationStudyElementRef = createdRealisation.getStudyElementReferences().get(0);
        assertEquals(studyElementReference.getReferenceIdentifier(), createdRealisationStudyElementRef.getReferenceIdentifier());
        assertEquals(studyElementReference.getReferenceOrganizer(), createdRealisationStudyElementRef.getReferenceOrganizer());
        assertEquals(StudyElementTypeEntity.ASSESSMENT_ITEM, createdRealisationStudyElementRef.getReferenceType());

        // Verify created realisation data is denormalized to course unit
        CourseUnitEntity updatedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).orElse(null);
        assertEquals(1, updatedCourseUnitEntity.getAssessmentItems().size());
        assertEquals(1, updatedCourseUnitEntity.getAssessmentItems().get(0).getRealisations().size());

        CourseUnitRealisationEntity denormalizedRealisation = updatedCourseUnitEntity.getAssessmentItems().get(0).getRealisations().get(0);
        assertEquals(createdRealisation.getRealisationId(), denormalizedRealisation.getRealisationId());
        assertEquals(createdRealisation.getRealisationIdentifierCode(), denormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(createdRealisation.getName().getValue("fi"), denormalizedRealisation.getName().getValue("fi"));
        assertEquals(createdRealisation.getName().getValue("en"), denormalizedRealisation.getName().getValue("en"));
        assertEquals(createdRealisation.getName().getValue("sv"), denormalizedRealisation.getName().getValue("sv"));
        assertEquals(createdRealisation.getEnrollmentStartDateTime(), denormalizedRealisation.getEnrollmentStartDateTime());
        assertEquals(createdRealisation.getEnrollmentEndDateTime(), denormalizedRealisation.getEnrollmentEndDateTime());
        assertEquals(createdRealisation.getStartDate(), denormalizedRealisation.getStartDate());
        assertEquals(createdRealisation.getEndDate(), denormalizedRealisation.getEndDate());
    }

    @Test
    public void testSendingCreateRealisationMessageWithoutCooperationNetworks_shouldFail() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        Realisation realisation = DtoInitializerV8.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
                Collections.singletonList(studyElementReference), Collections.emptyList(), Collections.singletonList(organisationReference));
        realisation.setStatus(StudyStatus.CANCELLED);

        CreateRealisationRequest req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        Message resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse response = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

        realisation = DtoInitializerV8.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
                Collections.singletonList(studyElementReference), null, Collections.singletonList(organisationReference));
        realisation.setStatus(StudyStatus.CANCELLED);

        req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        response = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

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
    public void testSendingDeleteRealisationMessage_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify one cu realisation exists in elasticsearch
        Optional<RealisationEntity> realisationEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", "TUNI");
        assertTrue(realisationEntity.isPresent());

        String deleteJson =
        """
            {
                "realisationId": "129177"
            }
        """;
        Message deleteResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, deleteJson, MessageType.DELETE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse deleteResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(deleteResponseMessage);
        assertTrue(deleteResp.getStatus() == Status.OK);
        assertEquals("Realisation deleted successfully", deleteResp.getMessage());

        // Verify course unit was deleted
        realisationEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", "TUNI");
        assertFalse(realisationEntity.isPresent());
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldFailDuplicateCURealisationInMessage() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
                "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testTwoSameRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
                "129177", "TUNI").orElse(null);
        assertNull(createdEntity);
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldFailAlreadyExists() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
                "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        RealisationEntity entity = new RealisationEntity();
        entity.setRealisationId("129177");
        entity.setRealisationIdentifierCode("129177");
        entity.setOrganizingOrganisationId(organisingOrganisationId);
        realisationRepository.create(entity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldFailMissingRequiredFields() throws JMSException, IOException {
        Realisation realisation = DtoInitializerV8.getRealisation("ID", "CODE",
                new LocalisedString("fi", null, null), null, null, null);

        CreateRealisationRequest request = new CreateRealisationRequest();
        request.setRealisations(Collections.singletonList(realisation));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, request, "TUNI");

        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);

        // Expected errors:
        // $.realisations[0].studyElementReferences: null found, array expected
        // $.realisations[0].organisationReferences: null found, array expected
        // $.realisations[0].cooperationNetworks: null found, array expected
        assertEquals(3, response.getErrors().size());
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
    public void testSendingCreateRealisationMessage_shouldFailCourseUnitAndRealisationNetworksNotMatching() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
                "CN-2", new LocalisedStringEntity("Verkosto 2", "Verkosto en", "Verkosto sv"), true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        RealisationEntity createdEntity = realisationService.findByIdAndOrganizingOrganisationId(
                "129177", "TUNI").orElse(null);
        assertNull(createdEntity);
    }

    @Test
    public void testSendingCreateRealisationMessageWithAssessmentItemRef_shouldFailNoCourseUnitFoundByAssessmentId() throws JMSException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
                "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "CU-1");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("COO-1", Collections.singletonList(assessmentItemEntity));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), organisingOrganisationId, "AI-2");

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetworkv8 = DtoInitializerV8.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Realisation realisation = DtoInitializerV8.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
                Collections.singletonList(studyElementReference), Collections.singletonList(cooperationNetworkv8), Collections.singletonList(organisationReference));

        CreateRealisationRequest req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        Message resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse response = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(
                realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNull(createdRealisation);
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
    public void testSendingDeleteRealisationMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify one cu realisation exists in elasticsearch
        Optional<RealisationEntity> realisationEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", "TUNI");
        assertTrue(realisationEntity.isPresent());

        String deleteJson =
        """
            {
                "realisationId": "129177"
            }
        """;
        Message deleteResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, deleteJson, MessageType.DELETE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse deleteResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(deleteResponseMessage);
        assertTrue(deleteResp.getStatus() == Status.OK);
        assertEquals("Realisation deleted successfully", deleteResp.getMessage());

        // Verify course unit was deleted
        realisationEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", "TUNI");
        assertFalse(realisationEntity.isPresent());

        List<RealisationEntity> historyEntities = HistoryHelper.queryHistoryIndex(
                elasticsearchTemplate, "toteutukset-history", RealisationEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        RealisationEntity historyEntity = historyEntities.get(0);
        assertNotNull(historyEntity.getCreatedTime());
        assertEquals("129177", historyEntity.getRealisationId());
        assertEquals("129177", historyEntity.getRealisationIdentifierCode());
        assertEquals(10, historyEntity.getMinSeats());
        assertEquals(400, historyEntity.getMaxSeats());
        assertEquals(historyEntity.getStudyElementReferences().size(), 1);
        assertTrue(historyEntity.getStudyElementReferences().stream().anyMatch(
                ser -> ser.getReferenceType() == StudyElementTypeEntity.COURSE_UNIT
                    && ser.getReferenceIdentifier().equals("CU-1")));
        assertEquals(2, historyEntity.getPersonReferences().size());
        assertTrue(historyEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertEquals(1, historyEntity.getCooperationNetworks().size());
        assertEquals(cooperationNetwork.getId(), historyEntity.getCooperationNetworks().get(0).getId());

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE.name(), createdEntity.getStatus().name());

        // Send update message for realisation to remove one teacher (identifier 7228) and update min seats to 20 and max seats to 200
        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateRealisationStatusJson, MessageType.UPDATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        RealisationEntity updatedEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", organisingOrganisationId)
                .orElse(null);
        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(createdEntity.getCreatedTime(), updatedEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.CANCELLED.name(), updatedEntity.getStatus().name());
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

    @Test
    public void testSendingDeleteRealisationMessage_courseUnitsHaveDenormalizedData_shouldRemoveCourseUnitDenormalizedData() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationEntity realisationEntityToDelete = EntityInitializer.getRealisationEntity("R1", "RCODE1", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntityToDelete.setName(new LocalisedStringEntity("Nimi 1", "Name 1", "Namn 1"));
        realisationEntityToDelete.setStartDate(LocalDate.now().minusMonths(1));
        realisationEntityToDelete.setEndDate(LocalDate.now().plusMonths(6));
        realisationEntityToDelete.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntityToDelete.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        // Add second realisation that is not deleted to verify no extra denormalized data is removed
        RealisationEntity realisationEntity2 = EntityInitializer.getRealisationEntity("R2", "RCODE2", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntity2.setName(new LocalisedStringEntity("Nimi 2", "Name 2", "Namn 2"));
        realisationEntity2.setStartDate(LocalDate.now().minusMonths(2));
        realisationEntity2.setEndDate(LocalDate.now().plusMonths(7));
        realisationEntity2.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity2.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(2));

        // Create course unit with realisation
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "CU1", "CODE1", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);

        CourseUnitRealisationEntity courseUnitRealisationEntity = CourseUnitRealisationEntity.from(realisationEntityToDelete);
        CourseUnitRealisationEntity courseUnitRealisationEntityNotRemoved = CourseUnitRealisationEntity.from(realisationEntity2);
        courseUnit.setRealisations(Arrays.asList(courseUnitRealisationEntity, courseUnitRealisationEntityNotRemoved));
        courseUnitRepository.create(courseUnit);

        // Create second course unit with assessment item realisation
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI1", "CU1");
        assessmentItemEntity.setRealisations(Arrays.asList(courseUnitRealisationEntity, courseUnitRealisationEntityNotRemoved));
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO1",
            Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitWithAssessmentItemRealisations = EntityInitializer.getCourseUnitEntity(
            "CU2", "CODE2", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitWithAssessmentItemRealisations.setCompletionOptions(Collections.singletonList(completionOptionEntity));
        courseUnitRepository.create(courseUnitWithAssessmentItemRealisations);

        // Add references for realisation to created course units
        StudyElementReferenceEntity courseUnitRef = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organisingOrganisationId);

        StudyElementReferenceEntity assessmentItemRef = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitWithAssessmentItemRealisations.getStudyElementId(), organisingOrganisationId, assessmentItemEntity.getAssessmentItemId());

        realisationEntityToDelete.setStudyElementReferences(Arrays.asList(assessmentItemRef, courseUnitRef));

        realisationRepository.create(realisationEntityToDelete);

        // Send delete message
        String deleteJson =
        """
            {
                "realisationId": "%s"
            }
        """.formatted(realisationEntityToDelete.getRealisationId());

        Message deleteResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, deleteJson, MessageType.DELETE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse deleteResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(deleteResponseMessage);
        assertTrue(deleteResp.getStatus() == Status.OK);
        assertEquals("Realisation deleted successfully", deleteResp.getMessage());

        // Verify one denormalized data still exists
        CourseUnitEntity courseUnitWithRealisations = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitWithRealisations);
        assertEquals(1, courseUnitWithRealisations.getRealisations().size());

        CourseUnitRealisationEntity denormalizedRealisation = courseUnitWithRealisations.getRealisations().get(0);
        assertEquals(courseUnitRealisationEntityNotRemoved.getRealisationId(), denormalizedRealisation.getRealisationId());
        assertEquals(courseUnitRealisationEntityNotRemoved.getRealisationIdentifierCode(), denormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("fi"), denormalizedRealisation.getName().getValue("fi"));
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("en"), denormalizedRealisation.getName().getValue("en"));
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("sv"), denormalizedRealisation.getName().getValue("sv"));
        assertEquals(DateUtils.getFormatted(courseUnitRealisationEntityNotRemoved.getEnrollmentStartDateTime()), DateUtils.getFormatted(denormalizedRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(courseUnitRealisationEntityNotRemoved.getEnrollmentEndDateTime()), DateUtils.getFormatted(denormalizedRealisation.getEnrollmentEndDateTime()));
        assertEquals(courseUnitRealisationEntityNotRemoved.getStartDate(), denormalizedRealisation.getStartDate());
        assertEquals(courseUnitRealisationEntityNotRemoved.getEndDate(), denormalizedRealisation.getEndDate());

        // Verify one denormalized data still exists
        CourseUnitEntity courseUnitWithAssessmentItemRealisation = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitWithAssessmentItemRealisations.getStudyElementId(), courseUnitWithAssessmentItemRealisations.getOrganizingOrganisationId()).orElse(null);

        assertNotNull(courseUnitWithAssessmentItemRealisation);
        assertEquals(1, courseUnitWithAssessmentItemRealisation.getAssessmentItemRealisations().size());

        CourseUnitRealisationEntity updatedDenormalizedRealisation = courseUnitWithAssessmentItemRealisation.getAssessmentItemRealisations().get(0);
        assertEquals(courseUnitRealisationEntityNotRemoved.getRealisationId(), updatedDenormalizedRealisation.getRealisationId());
        assertEquals(courseUnitRealisationEntityNotRemoved.getRealisationIdentifierCode(), updatedDenormalizedRealisation.getRealisationIdentifierCode());
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("fi"), updatedDenormalizedRealisation.getName().getValue("fi"));
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("en"), updatedDenormalizedRealisation.getName().getValue("en"));
        assertEquals(courseUnitRealisationEntityNotRemoved.getName().getValue("sv"), updatedDenormalizedRealisation.getName().getValue("sv"));
        assertEquals(DateUtils.getFormatted(courseUnitRealisationEntityNotRemoved.getEnrollmentStartDateTime()), DateUtils.getFormatted(updatedDenormalizedRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(courseUnitRealisationEntityNotRemoved.getEnrollmentEndDateTime()), DateUtils.getFormatted(updatedDenormalizedRealisation.getEnrollmentEndDateTime()));
        assertEquals(courseUnitRealisationEntityNotRemoved.getStartDate(), updatedDenormalizedRealisation.getStartDate());
        assertEquals(courseUnitRealisationEntityNotRemoved.getEndDate(), updatedDenormalizedRealisation.getEndDate());
    }

    private NetworkEntity persistNetworkEntity(String id, LocalisedStringEntity name,
                                               List<String> organisationIds) {
        List<NetworkOrganisationEntity> networkOrgs = new ArrayList<>();
        for (String orgId : organisationIds) {
            NetworkOrganisationEntity networkOrganisation = new NetworkOrganisationEntity();
            networkOrganisation.setOrganisationTkCode(orgId);
            networkOrganisation.setValidityInNetwork(EntityInitializer.getIndefinitelyValidity(OffsetDateTime.now()));
            networkOrganisation.setIsCoordinator(true);
            networkOrgs.add(networkOrganisation);
        }
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(id, name, networkOrgs,
                EntityInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        return networkRepository.create(networkEntity);
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
                    "teachingLanguage": [
                        "EN"
                    ],
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

    private final String twoRealisationsJson =
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
                                "surName": "Testinen2"
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
                   ]
                },
                {
                    "status": "ACTIVE",
                    "realisationId": "129178",
                    "realisationIdentifierCode": "129178",
                    "selections": null,
                    "questionSets": null,
                    "enrollmentStartDateTime": "2018-09-30T21:00:00.000Z",
                    "enrollmentEndDateTime": "2018-12-04T22:00:00.000Z",
                    "startDate": "2019-01-06",
                    "endDate": "2019-05-30",
                    "minSeats": 10,
                    "maxSeats": 400,
                    "name": {
                       "values": {
                            "fi": "nimi 2 fi",
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
                            "referenceIdentifier": "CU-2",
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
                   ]
                }
            ]
        }
    """;

    private final String testTwoSameRealisationJson =
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
                    "minSeats": 10,
                    "maxSeats": 400,
                    "name": {
                       "values": {
                            "fi": "nimi 2 fi",
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
                   ]
                },
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
                    "minSeats": 10,
                    "maxSeats": 400,
                    "name": {
                       "values": {
                            "fi": "nimi 2 fi",
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
                   ]
                }
            ]
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
