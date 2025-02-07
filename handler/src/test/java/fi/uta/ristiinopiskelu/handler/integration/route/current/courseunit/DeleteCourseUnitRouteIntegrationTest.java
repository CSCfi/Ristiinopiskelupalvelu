package fi.uta.ristiinopiskelu.handler.integration.route.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.HistoryHelper;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.DeleteCourseUnitRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class DeleteCourseUnitRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(DeleteCourseUnitRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

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
                organisation.setSchemaVersion(this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingDeleteCourseUnitMessage_shouldDeleteOneOfThreeCourseUnits() throws JMSException {
        String id1 = "ID1";
        String code1 = "RAIRAI1";
        String code2 = "RAIRAI2";
        String organisationId1 = "ORG-1";
        String organisationId2 = "ORG-2";

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity(organisationId1, null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnit1 = EntityInitializer.getCourseUnitEntity(id1, code1, organisationId1, null,
            new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv"));

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("ID2", code2, organisationId1, null,
            new LocalisedString("jaksonnimi 2 fi", "jaksonnimi 2 en", "jaksonnimi 2 sv"));

        CourseUnitEntity courseUnit3 = EntityInitializer.getCourseUnitEntity("ID3", code1, organisationId2, null,
            new LocalisedString("jaksonnimi 3 fi", "jaksonnimi 3 en", "jaksonnimi 3 sv"));

        courseUnitRepository.create(courseUnit1);
        courseUnitRepository.create(courseUnit2);
        courseUnitRepository.create(courseUnit3);

        // Verify there is 3 course units in ES before deleting
        List<CourseUnitEntity> courseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertEquals(3, courseUnits.size());

        DeleteCourseUnitRequest request = new DeleteCourseUnitRequest();
        request.setStudyElementId(id1);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, request, organisationId1);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        courseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertEquals(2, courseUnits.size());
        assertTrue(!courseUnits.stream().anyMatch(cu -> cu.getOrganizingOrganisationId().equals(organisationId1) && cu.getStudyElementIdentifierCode().equals(code1)));
        assertTrue(courseUnits.stream().anyMatch(cu -> cu.getOrganizingOrganisationId().equals(organisationId1) && cu.getStudyElementIdentifierCode().equals(code2)));
        assertTrue(courseUnits.stream().anyMatch(cu -> cu.getOrganizingOrganisationId().equals(organisationId2) && cu.getStudyElementIdentifierCode().equals(code1)));
    }


    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldSuccess() throws JMSException {

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));
        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", "1",
            Collections.singletonList(new StudyElementReference("1", "1", StudyElementType.COURSE_UNIT)),
            Collections.emptyList());

        organisationService.create(organisationEntity);
        courseUnitRepository.create(courseUnit);
        realisationRepository.create(realisation);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("1");
        req.setDeleteRealisations(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        assertEquals(0, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(0, realisationService.findAll(Pageable.unpaged()).size());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithMultipleRealisations_studyElementReferencesRemoved_shouldSucceed() throws JMSException {

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "TEST1", "1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("CU2", "TEST2", "1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("REAL1", "REAL1", "1",
            Collections.singletonList(new StudyElementReference("CU1", "1", StudyElementType.COURSE_UNIT)),
            Collections.emptyList());

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("REAL2", "REAL2", "1",
            Arrays.asList(
                new StudyElementReference("CU1", "1", StudyElementType.COURSE_UNIT),
                new StudyElementReference("CU2", "1", StudyElementType.COURSE_UNIT)),
            Collections.emptyList());

        organisationService.create(organisationEntity);
        courseUnitRepository.create(courseUnit);
        courseUnitRepository.create(courseUnit2);
        realisationRepository.create(realisation);
        realisationRepository.create(realisation2);

        List<CourseUnitEntity> courseUnitEntities = courseUnitService.findAll(Pageable.unpaged());
        assertEquals(2, courseUnitEntities.size());
        assertThat(courseUnitEntities, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnit.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnit2.getStudyElementId()))
        ));

        List<RealisationEntity> realisationEntities = realisationService.findAll(Pageable.unpaged());
        assertEquals(2, realisationEntities.size());
        assertThat(realisationEntities, containsInAnyOrder(
            hasProperty("realisationId", is(realisation.getRealisationId())),
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("CU1");
        req.setDeleteRealisations(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        courseUnitEntities = courseUnitService.findAll(Pageable.unpaged());
        assertEquals(1, courseUnitEntities.size());
        assertThat(courseUnitEntities, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnit2.getStudyElementId()))
        ));

        realisationEntities = realisationService.findAll(Pageable.unpaged());
        assertEquals(1, realisationEntities.size());
        assertThat(realisationEntities, containsInAnyOrder(
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        List<StudyElementReference> references = realisationEntities.get(0).getStudyElementReferences();
        assertEquals(1, references.size());
        assertEquals(courseUnit2.getStudyElementId(), references.get(0).getReferenceIdentifier());
        assertEquals(courseUnit2.getOrganizingOrganisationId(), references.get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.COURSE_UNIT, references.get(0).getReferenceType());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldFail() throws JMSException {

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));
        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", "1",
            Collections.singletonList(new StudyElementReference("1",  "1", StudyElementType.COURSE_UNIT)), Collections.emptyList());

        courseUnitRepository.create(courseUnit);
        realisationRepository.create(realisation);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("1");

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithMultipleCourseUnitReferences_shouldSucceed() throws JMSException {

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("ORG1", null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "ORG1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));

        List<StudyElementReference> references = new ArrayList<>();
        references.add(new StudyElementReference("CU1",  "ORG1", StudyElementType.COURSE_UNIT));
        references.add(new StudyElementReference("CU2",  "ORG2", StudyElementType.COURSE_UNIT));

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("RS1", "RS1_CODE1", "ORG1",
            references,
            Collections.emptyList());

        courseUnitRepository.create(courseUnit);
        realisationRepository.create(realisation);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("CU1");
        req.setDeleteRealisations(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "ORG1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<RealisationEntity> realisations = realisationService.findAll(Pageable.unpaged());

        assertEquals(0, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisations.size());

        assertEquals("RS1", realisations.get(0).getRealisationId());
        assertEquals("RS1_CODE1", realisations.get(0).getRealisationIdentifierCode());
        assertEquals("ORG1", realisations.get(0).getOrganizingOrganisationId());

        assertEquals(1, realisations.get(0).getStudyElementReferences().size());
        assertEquals("CU2", realisations.get(0).getStudyElementReferences().get(0).getReferenceIdentifier());
        assertEquals("ORG2", realisations.get(0).getStudyElementReferences().get(0).getReferenceOrganizer());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisationsReference_shouldSuccessAndAddHistoryForRealisation() throws JMSException {
        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "1",
            "TEST1",
            "1",
            Collections.emptyList(),
            new LocalisedString("test", "test", "test"));

        StudyElementReference courseUnitRef = new StudyElementReference(courseUnit.getStudyElementId(),
            courseUnit.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);

        StudyElementReference extraRef = new StudyElementReference("123",  "1", StudyElementType.COURSE_UNIT);

        RealisationEntity realisation = EntityInitializer.getRealisationEntity(
            "2",
            "TEST2",
            "1",
            Arrays.asList(courseUnitRef, extraRef),
            Collections.emptyList());

        courseUnitRepository.create(courseUnit);
        realisationRepository.create(realisation);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("1");
        req.setDeleteRealisations(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        assertEquals(0, courseUnitService.findAll(Pageable.unpaged()).size());

        RealisationEntity realisationEntity = realisationService
            .findByIdAndOrganizingOrganisationId("2", "1")
            .orElse(null);

        assertNotNull(realisationEntity);
        assertEquals(1, realisationEntity.getStudyElementReferences().size());
        assertNotEquals(courseUnit.getStudyElementId(), realisationEntity.getStudyElementReferences().get(0).getReferenceIdentifier());

        List<RealisationEntity> historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate,"toteutukset-history", RealisationEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        RealisationEntity historyEntity = historyEntities.get(0);
        assertEquals(realisationEntity.getRealisationId(), historyEntity.getRealisationId());
        assertEquals(realisationEntity.getRealisationIdentifierCode(), historyEntity.getRealisationIdentifierCode());
        assertEquals(realisationEntity.getOrganizingOrganisationId(), historyEntity.getOrganizingOrganisationId());
        assertTrue(historyEntity.getStudyElementReferences().stream().anyMatch(
            sr -> sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())));
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldSuccessAndCreateHistory() throws JMSException {

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
            Collections.emptyList(), new LocalisedString("test", "test", "test"));

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(new StudyElementReference(courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT)),
            Collections.emptyList());

        courseUnitRepository.create(courseUnit);
        realisationRepository.create(realisation);

        assertEquals(1, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(1, realisationService.findAll(Pageable.unpaged()).size());

        DeleteCourseUnitRequest req = new DeleteCourseUnitRequest();
        req.setStudyElementId("1");
        req.setDeleteRealisations(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        assertEquals(0, courseUnitService.findAll(Pageable.unpaged()).size());
        assertEquals(0, realisationService.findAll(Pageable.unpaged()).size());

        List<CourseUnitEntity> cuHistoryEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(cuHistoryEntities);
        assertEquals(1, cuHistoryEntities.size());

        CourseUnitEntity cuHistoryEntity = cuHistoryEntities.get(0);
        assertEquals(DateUtils.getFormatted(courseUnit.getCreatedTime()), DateUtils.getFormatted(cuHistoryEntity.getCreatedTime()));
        assertEquals(courseUnit.getStudyElementId(), cuHistoryEntity.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), cuHistoryEntity.getStudyElementIdentifierCode());
        assertEquals(courseUnit.getOrganizingOrganisationId(), cuHistoryEntity.getOrganizingOrganisationId());
        assertEquals(courseUnit.getName().getValue("fi"), cuHistoryEntity.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("sv"), cuHistoryEntity.getName().getValue("sv"));
        assertEquals(courseUnit.getName().getValue("en"), cuHistoryEntity.getName().getValue("en"));

        List<RealisationEntity> realisationHistoryEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate, "toteutukset-history", RealisationEntity.class);
        assertNotNull(realisationHistoryEntities);
        assertEquals(1, realisationHistoryEntities.size());

        RealisationEntity realisationHistoryEntity = realisationHistoryEntities.get(0);
        assertEquals(DateUtils.getFormatted(realisation.getCreatedTime()), DateUtils.getFormatted(realisationHistoryEntity.getCreatedTime()));
        assertEquals(realisation.getRealisationId(), realisationHistoryEntity.getRealisationId());
        assertEquals(realisation.getRealisationIdentifierCode(), realisationHistoryEntity.getRealisationIdentifierCode());
        assertEquals(1, realisationHistoryEntity.getStudyElementReferences().size());
        assertTrue(realisationHistoryEntity.getStudyElementReferences().stream().anyMatch(
            sr -> sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())
                && sr.getReferenceType() == StudyElementType.COURSE_UNIT));
    }
}
