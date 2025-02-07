package fi.uta.ristiinopiskelu.handler.integration.route.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
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
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class DeleteRealisationRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(DeleteRealisationRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

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

    @Value("${general.message-schema.version.current}")
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
    public void testSendingDeleteRealisationMessage_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
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

        String deleteJson = "{\n" +
            "    \"realisationId\": \"129177\"\n" +
            "}";
        Message deleteResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, deleteJson, MessageType.DELETE_REALISATION_REQUEST.name(), organisingOrganisationId);

        DefaultResponse deleteResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(deleteResponseMessage);
        assertTrue(deleteResp.getStatus() == Status.OK);
        assertEquals("Realisation deleted successfully", deleteResp.getMessage());

        // Verify course unit was deleted
        realisationEntity = realisationService.findByIdAndOrganizingOrganisationId("129177", "TUNI");
        assertFalse(realisationEntity.isPresent());
    }

    @Test
    public void testSendingDeleteRealisationMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(networkEntity.getId(), networkEntity.getName(),
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

        String deleteJson = "{\n" +
            "    \"realisationId\": \"129177\"\n" +
            "}";
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
            ser -> ser.getReferenceType() == StudyElementType.COURSE_UNIT
                && ser.getReferenceIdentifier().equals("CU-1")));
        assertEquals(2, historyEntity.getPersonReferences().size());
        assertTrue(historyEntity.getPersonReferences().stream().allMatch(pr -> pr.getPerson() != null));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi@test.fi")));
        assertTrue(historyEntity.getPersonReferences().stream().anyMatch(pr -> pr.getPerson().getHomeEppn().equals("testi2@test.fi")));
        assertEquals(1, historyEntity.getCooperationNetworks().size());
        assertEquals(cooperationNetwork.getId(), historyEntity.getCooperationNetworks().get(0).getId());

    }

    @Test
    public void testSendingDeleteRealisationMessage_courseUnitsHaveDenormalizedData_shouldRemoveCourseUnitDenormalizedData() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationEntity realisationEntityToDelete = EntityInitializer.getRealisationEntity("R1", "RCODE1", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntityToDelete.setName(new LocalisedString("Nimi 1", "Name 1", "Namn 1"));
        realisationEntityToDelete.setStartDate(LocalDate.now().minusMonths(1));
        realisationEntityToDelete.setEndDate(LocalDate.now().plusMonths(6));
        realisationEntityToDelete.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntityToDelete.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        // Add second realisation that is not deleted to verify no extra denormalized data is removed
        RealisationEntity realisationEntity2 = EntityInitializer.getRealisationEntity("R2", "RCODE2", "TUNI",
            null, Collections.singletonList(cooperationNetwork), null);
        realisationEntity2.setName(new LocalisedString("Nimi 2", "Name 2", "Namn 2"));
        realisationEntity2.setStartDate(LocalDate.now().minusMonths(2));
        realisationEntity2.setEndDate(LocalDate.now().plusMonths(7));
        realisationEntity2.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity2.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(2));

        // Create course unit with realisation
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "CU1", "CODE1", organisingOrganisationId, Collections.singletonList(cooperationNetwork), null);

        CourseUnitRealisationEntity courseUnitRealisationEntity = modelMapper.map(realisationEntityToDelete, CourseUnitRealisationEntity.class);
        CourseUnitRealisationEntity courseUnitRealisationEntityNotRemoved = modelMapper.map(realisationEntity2, CourseUnitRealisationEntity.class);
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
        StudyElementReference courseUnitRef = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organisingOrganisationId);

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitWithAssessmentItemRealisations.getStudyElementId(), organisingOrganisationId, assessmentItemEntity.getAssessmentItemId());

        realisationEntityToDelete.setStudyElementReferences(Arrays.asList(assessmentItemRef, courseUnitRef));

        realisationRepository.create(realisationEntityToDelete);

        // Send delete message
        String deleteJson = "{\n" +
            "    \"realisationId\": \"" + realisationEntityToDelete.getRealisationId()  + "\"\n" +
            "}";

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
}
