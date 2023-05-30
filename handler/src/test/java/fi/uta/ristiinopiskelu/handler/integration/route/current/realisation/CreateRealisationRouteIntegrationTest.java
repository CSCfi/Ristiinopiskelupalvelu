package fi.uta.ristiinopiskelu.handler.integration.route.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializerV8;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class CreateRealisationRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateRealisationRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Value("${general.messageSchema.version}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), null,
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
        assertEquals(StudyStatus.ACTIVE, createdRealisationEntity.getStatus());
        assertEquals(BigDecimal.valueOf(5.0), createdRealisationEntity.getCreditsMin());
        assertEquals(BigDecimal.valueOf(15.0), createdRealisationEntity.getCreditsMax());

        // Verify cooperation network data was gathered from network index
        CooperationNetwork realisationNetwork = createdRealisationEntity.getCooperationNetworks().get(0);
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
    public void testSendingCreateRealisationMessageWithInvalidLanguage_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        // first with valid language
        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, testRealisationJson, MessageType.CREATE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // then with invalid language
        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("REAL/11", "REALCODE/11", new LocalisedString("toteutus", null, null),
            Collections.singletonList(studyElementReference), Arrays.asList(cooperationNetwork), Collections.singletonList(orgRef));
        realisation.setStatus(StudyStatus.ACTIVE);
        realisation.setTeachingLanguage(Collections.singletonList("foofoo"));

        CreateRealisationRequest realisationRequest = new CreateRealisationRequest();
        realisationRequest.setRealisations(Collections.singletonList(realisation));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, realisationRequest, organisingOrganisationId);

        JsonValidationFailedResponse jsonValidationFailedResponse = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(jsonValidationFailedResponse.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateRealisationMessageV8WithInvalidLanguage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        // test v8 api. teaching language should be set to empty because no valid language was given
        fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElementReference studyElementReferenceV8 = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation organisationV8 = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference orgRefV8 = DtoInitializerV8.getOrganisationReference(organisationV8,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole.ROLE_MAIN_ORGANIZER);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation realisationV8 = DtoInitializerV8.getRealisation("REAL/11", "REALCODE/11",
            new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("toteutus", null, null),
            Collections.singletonList(studyElementReferenceV8),
            Collections.singletonList(DtoInitializerV8.getCooperationNetwork(networkEntity.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))),
            Collections.singletonList(orgRefV8));
        realisationV8.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE);
        realisationV8.setTeachingLanguage(Collections.singletonList("foofoo"));

        fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest realisationRequestV8 =
            new fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest();
        realisationRequestV8.setRealisations(Collections.singletonList(realisationV8));

        JmsHelper.setMessageSchemaVersion(8);
        OrganisationEntity organisationEntity = organisationService.findById("TUNI").get();
        organisationEntity.setSchemaVersion(8);
        organisationService.update(organisationEntity);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, realisationRequestV8, organisingOrganisationId);

        fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse v8response =
            (fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(v8response.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);

        RealisationEntity updated = realisationRepository.findByRealisationIdAndOrganizingOrganisationId("REAL/11", "TUNI").get();
        assertEquals(Collections.emptyList(), updated.getTeachingLanguage());
    }

    @Test
    public void testSendingCreateRealisationMessage_shouldSuccessHasForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        NetworkEntity networkEntity2 = persistNetworkEntity("CN-2",
            new LocalisedString("Verkosto 2", "Verkosto 2 en", "Verkosto 2 sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork cooperationNetwork2 = DtoInitializer.getCooperationNetwork(networkEntity2.getId(), null,
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU/1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("REAL/11", "REALCODE/11", new LocalisedString("toteutus", null, null),
            Collections.singletonList(studyElementReference), Arrays.asList(cooperationNetwork, cooperationNetwork2), Collections.singletonList(orgRef));
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
        assertEquals(StudyStatus.ARCHIVED, createdEntity.getStatus());

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
    public void testCreateRealisationMessageWithMultipleCUReals_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
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
        assertEquals(StudyStatus.ACTIVE, createdEntity.getStatus());


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
        assertEquals(StudyStatus.ACTIVE, createdEntity2.getStatus());
    }

    @Test
    public void testSendingCreateRealisationMessageWithAssessmentItemRef_shouldSuccess() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "CU-1");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("COO-1", Collections.singletonList(assessmentItemEntity));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU-1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId, assessmentItemEntity.getAssessmentItemId());

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
            Collections.singletonList(studyElementReference), Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference));
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
        assertEquals(StudyStatus.CANCELLED, createdRealisation.getStatus());

        StudyElementReference createdRealisationStudyElementRef = createdRealisation.getStudyElementReferences().get(0);
        assertEquals(studyElementReference.getReferenceIdentifier(), createdRealisationStudyElementRef.getReferenceIdentifier());
        assertEquals(studyElementReference.getReferenceOrganizer(), createdRealisationStudyElementRef.getReferenceOrganizer());
        assertEquals(studyElementReference.getReferenceType(), createdRealisationStudyElementRef.getReferenceType());

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
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU-1", "4CO19/KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId);

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
            Collections.singletonList(studyElementReference), Collections.emptyList(), Collections.singletonList(organisationReference));
        realisation.setStatus(StudyStatus.CANCELLED);

        CreateRealisationRequest req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        Message resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse response = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

        realisation = DtoInitializer.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
            Collections.singletonList(studyElementReference), null, Collections.singletonList(organisationReference));
        realisation.setStatus(StudyStatus.CANCELLED);

        req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        response = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

    }

    @Test
    public void testSendingCreateRealisationMessage_shouldFailDuplicateCURealisationInMessage() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
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

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
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
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("ID", "CODE",
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
    public void testSendingCreateRealisationMessage_shouldFailCourseUnitAndRealisationNetworksNotMatching() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-2", new LocalisedString("Verkosto 2", "Verkosto en", "Verkosto sv"), true,
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

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "CU-1");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("COO-1", Collections.singletonList(assessmentItemEntity));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "CU-1", "4CO19KBIOP", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitEntity.getStudyElementId(), organisingOrganisationId, "AI-2");

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("R-1", "RCODE-1", new LocalisedString("Toteutus", null, null),
            Collections.singletonList(studyElementReference), Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference));

        CreateRealisationRequest req = new CreateRealisationRequest();
        req.setRealisations(Collections.singletonList(realisation));

        Message resp = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse response = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(resp);
        assertTrue(response.getStatus() == Status.FAILED);

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(
            realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNull(createdRealisation);
    }

    private final String testRealisationJson =
        "{\n" +
            "    \"realisations\": [\n" +
            "        {\n" +
            "            \"status\": \"ACTIVE\",\n" +
            "            \"realisationId\": \"129177\",\n" +
            "            \"realisationIdentifierCode\": \"129177\",\n" +
            "            \"selections\": null,\n" +
            "            \"questionSets\": null,\n" +
            "            \"enrollmentStartDateTime\": \"2018-09-30T21:00:00.000Z\",\n" +
            "            \"enrollmentEndDateTime\": \"2018-12-04T22:00:00.000Z\",\n" +
            "            \"startDate\": \"2019-01-06\",\n" +
            "            \"endDate\": \"2019-05-30\",\n" +
            "            \"creditsMin\": 5,\n" +
            "            \"creditsMax\": 15,\n" +
            "            \"minSeats\": 10,\n" +
            "            \"maxSeats\": 400,\n" +
            "            \"name\": {\n" +
            "               \"values\": {\n" +
            "                    \"fi\": \"nimi fi\",\n" +
            "                    \"sv\": null,\n" +
            "                    \"en\": null\n" +
            "                }\n" +
            "             },\n" +
            "            \"personReferences\": [\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi2@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"studyElementReferences\": [\n" +
            "               {\n" +
            "                    \"referenceIdentifier\": \"CU-1\",\n" +
            "                    \"referenceOrganizer\": \"TUNI\",\n" +
            "                    \"referenceType\": \"COURSE_UNIT\"\n" +
            "               }\n" +
            "            ],\n" +
            "           \"organisationReferences\": [\n" +
            "               {\n" +
            "                   \"organisationRole\": 1,\n" +
            "                   \"target\": {\n" +
            "                       \"organisationIdentifier\": \"TUNI\",\n" +
            "                       \"organisationTkCode\": \"TUNI\"\n" +
            "                   }\n" +
            "               }\n" +
            "           ],\n" +
            "           \"cooperationNetworks\": [\n" +
            "               {\n" +
            "                   \"id\": \"CN-1\",\n" +
            "                   \"name\": {\n" +
            "                       \"values\": {\n" +
            "                           \"fi\": \"Verkosto 1\",\n" +
            "                           \"en\": \"Network 1\",\n" +
            "                           \"sv\": null\n" +
            "                       }\n" +
            "                   },\n" +
            "                   \"enrollable\": true\n" +
            "               }\n" +
            "           ],\n" +
            "           \"teachingLanguage\": [\n" +
            "               \"fi\"\n" +
            "           ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String testTwoSameRealisationJson =
        "{\n" +
            "    \"realisations\": [\n" +
            "        {\n" +
            "            \"status\": \"ACTIVE\",\n" +
            "            \"realisationId\": \"129177\",\n" +
            "            \"realisationIdentifierCode\": \"129177\",\n" +
            "            \"selections\": null,\n" +
            "            \"questionSets\": null,\n" +
            "            \"enrollmentStartDateTime\": \"2018-09-30T21:00:00.000Z\",\n" +
            "            \"enrollmentEndDateTime\": \"2018-12-04T22:00:00.000Z\",\n" +
            "            \"startDate\": \"2019-01-06\",\n" +
            "            \"endDate\": \"2019-05-30\",\n" +
            "            \"minSeats\": 10,\n" +
            "            \"maxSeats\": 400,\n" +
            "            \"name\": {\n" +
            "               \"values\": {\n" +
            "                    \"fi\": \"nimi 2 fi\",\n" +
            "                    \"sv\": null,\n" +
            "                    \"en\": null\n" +
            "                }\n" +
            "             },\n" +
            "            \"personReferences\": [\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi2@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"studyElementReferences\": [\n" +
            "               {\n" +
            "                    \"referenceIdentifier\": \"CU-1\",\n" +
            "                    \"referenceOrganizer\": \"TUNI\",\n" +
            "                    \"referenceType\": \"COURSE_UNIT\"\n" +
            "               }\n" +
            "            ],\n" +
            "           \"organisationReferences\": [\n" +
            "               {\n" +
            "                   \"organisationRole\": 1,\n" +
            "                   \"target\": {\n" +
            "                       \"organisationIdentifier\": \"TUNI\",\n" +
            "                       \"organisationTkCode\": \"TUNI\"\n" +
            "                   }\n" +
            "               }\n" +
            "           ],\n" +
            "           \"cooperationNetworks\": [\n" +
            "               {\n" +
            "                   \"id\": \"CN-1\",\n" +
            "                   \"name\": {\n" +
            "                       \"values\": {\n" +
            "                           \"fi\": \"Verkosto 1\",\n" +
            "                           \"en\": \"Network 1\",\n" +
            "                           \"sv\": null\n" +
            "                       }\n" +
            "                   },\n" +
            "                   \"enrollable\": true\n" +
            "               }\n" +
            "           ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"status\": \"ACTIVE\",\n" +
            "            \"realisationId\": \"129177\",\n" +
            "            \"realisationIdentifierCode\": \"129177\",\n" +
            "            \"selections\": null,\n" +
            "            \"questionSets\": null,\n" +
            "            \"enrollmentStartDateTime\": \"2018-09-30T21:00:00.000Z\",\n" +
            "            \"enrollmentEndDateTime\": \"2018-12-04T22:00:00.000Z\",\n" +
            "            \"startDate\": \"2019-01-06\",\n" +
            "            \"endDate\": \"2019-05-30\",\n" +
            "            \"minSeats\": 10,\n" +
            "            \"maxSeats\": 400,\n" +
            "            \"name\": {\n" +
            "               \"values\": {\n" +
            "                    \"fi\": \"nimi 2 fi\",\n" +
            "                    \"sv\": null,\n" +
            "                    \"en\": null\n" +
            "                }\n" +
            "             },\n" +
            "            \"personReferences\": [\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi2@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"studyElementReferences\": [\n" +
            "               {\n" +
            "                    \"referenceIdentifier\": \"CU-1\",\n" +
            "                    \"referenceOrganizer\": \"TUNI\",\n" +
            "                    \"referenceType\": \"COURSE_UNIT\"\n" +
            "               }\n" +
            "            ],\n" +
            "           \"organisationReferences\": [\n" +
            "               {\n" +
            "                   \"organisationRole\": 1,\n" +
            "                   \"target\": {\n" +
            "                       \"organisationIdentifier\": \"TUNI\",\n" +
            "                       \"organisationTkCode\": \"TUNI\"\n" +
            "                   }\n" +
            "               }\n" +
            "           ],\n" +
            "           \"cooperationNetworks\": [\n" +
            "               {\n" +
            "                   \"id\": \"CN-1\",\n" +
            "                   \"name\": {\n" +
            "                       \"values\": {\n" +
            "                           \"fi\": \"Verkosto 1\",\n" +
            "                           \"en\": \"Network 1\",\n" +
            "                           \"sv\": null\n" +
            "                       }\n" +
            "                   },\n" +
            "                   \"enrollable\": true\n" +
            "               }\n" +
            "           ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String twoRealisationsJson =
        "{\n" +
            "    \"realisations\": [\n" +
            "        {\n" +
            "            \"status\": \"ACTIVE\",\n" +
            "            \"realisationId\": \"129177\",\n" +
            "            \"realisationIdentifierCode\": \"129177\",\n" +
            "            \"selections\": null,\n" +
            "            \"questionSets\": null,\n" +
            "            \"enrollmentStartDateTime\": \"2018-09-30T21:00:00.000Z\",\n" +
            "            \"enrollmentEndDateTime\": \"2018-12-04T22:00:00.000Z\",\n" +
            "            \"startDate\": \"2019-01-06\",\n" +
            "            \"endDate\": \"2019-05-30\",\n" +
            "            \"minSeats\": 10,\n" +
            "            \"maxSeats\": 400,\n" +
            "            \"name\": {\n" +
            "               \"values\": {\n" +
            "                    \"fi\": \"nimi fi\",\n" +
            "                    \"sv\": null,\n" +
            "                    \"en\": null\n" +
            "                }\n" +
            "             },\n" +
            "            \"personReferences\": [\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi2@test.fi\",\n" +
            "                        \"surName\": \"Testinen2\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"studyElementReferences\": [\n" +
            "               {\n" +
            "                    \"referenceIdentifier\": \"CU-1\",\n" +
            "                    \"referenceOrganizer\": \"TUNI\",\n" +
            "                    \"referenceType\": \"COURSE_UNIT\"\n" +
            "               }\n" +
            "            ],\n" +
            "           \"organisationReferences\": [\n" +
            "               {\n" +
            "                   \"organisationRole\": 1,\n" +
            "                   \"target\": {\n" +
            "                       \"organisationIdentifier\": \"TUNI\",\n" +
            "                       \"organisationTkCode\": \"TUNI\"\n" +
            "                   }\n" +
            "               }\n" +
            "           ],\n" +
            "           \"cooperationNetworks\": [\n" +
            "               {\n" +
            "                   \"id\": \"CN-1\",\n" +
            "                   \"name\": {\n" +
            "                       \"values\": {\n" +
            "                           \"fi\": \"Verkosto 1\",\n" +
            "                           \"en\": \"Network 1\",\n" +
            "                           \"sv\": null\n" +
            "                       }\n" +
            "                   },\n" +
            "                   \"enrollable\": true\n" +
            "               }\n" +
            "           ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"status\": \"ACTIVE\",\n" +
            "            \"realisationId\": \"129178\",\n" +
            "            \"realisationIdentifierCode\": \"129178\",\n" +
            "            \"selections\": null,\n" +
            "            \"questionSets\": null,\n" +
            "            \"enrollmentStartDateTime\": \"2018-09-30T21:00:00.000Z\",\n" +
            "            \"enrollmentEndDateTime\": \"2018-12-04T22:00:00.000Z\",\n" +
            "            \"startDate\": \"2019-01-06\",\n" +
            "            \"endDate\": \"2019-05-30\",\n" +
            "            \"minSeats\": 10,\n" +
            "            \"maxSeats\": 400,\n" +
            "            \"name\": {\n" +
            "               \"values\": {\n" +
            "                    \"fi\": \"nimi 2 fi\",\n" +
            "                    \"sv\": null,\n" +
            "                    \"en\": null\n" +
            "                }\n" +
            "             },\n" +
            "            \"personReferences\": [\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"personRole\": {\n" +
            "                        \"key\": \"teacher\",\n" +
            "                        \"codeSetKey\": \"personRole\"\n" +
            "                    },\n" +
            "                    \"person\": {\n" +
            "                        \"homeEppn\": \"testi2@test.fi\",\n" +
            "                        \"surName\": \"Testinen\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"studyElementReferences\": [\n" +
            "               {\n" +
            "                    \"referenceIdentifier\": \"CU-2\",\n" +
            "                    \"referenceOrganizer\": \"TUNI\",\n" +
            "                    \"referenceType\": \"COURSE_UNIT\"\n" +
            "               }\n" +
            "            ],\n" +
            "           \"organisationReferences\": [\n" +
            "               {\n" +
            "                   \"organisationRole\": 1,\n" +
            "                   \"target\": {\n" +
            "                       \"organisationIdentifier\": \"TUNI\",\n" +
            "                       \"organisationTkCode\": \"TUNI\"\n" +
            "                   }\n" +
            "               }\n" +
            "           ],\n" +
            "           \"cooperationNetworks\": [\n" +
            "               {\n" +
            "                   \"id\": \"CN-1\",\n" +
            "                   \"name\": {\n" +
            "                       \"values\": {\n" +
            "                           \"fi\": \"Verkosto 1\",\n" +
            "                           \"en\": \"Network 1\",\n" +
            "                           \"sv\": null\n" +
            "                       }\n" +
            "                   },\n" +
            "                   \"enrollable\": true\n" +
            "               }\n" +
            "           ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";
}
