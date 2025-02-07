package fi.uta.ristiinopiskelu.handler.integration.route.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
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
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.DeleteStudyModuleRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class DeleteStudyModuleRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudyModuleRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private StudyModuleService studyModuleService;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI", "METROP", "LAUREA");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }


    @Test
    public void testSendingDeleteStudyModuleMessage_shouldSuccess() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity);

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId(studyModuleEntity.getStudyElementId());

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity entityShouldBeNull = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNull(entityShouldBeNull);
    }

    @Test
    public void testSendingDeleteStudyModuleMessageWithCourseUnits_shouldSuccess() throws JMSException {

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "TUNI", Collections.emptyList(), new LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "TUNI", Collections.emptyList(), new LocalisedString("test", "test", "test"));
        courseUnit.setParents(Collections.singletonList(
            new StudyElementReference("SM1", "TUNI", StudyElementType.STUDY_MODULE)));

        studyModuleRepository.create(studyModule);
        courseUnitRepository.create(courseUnit);

        List<CourseUnitEntity> courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(1, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(1, courseUnits.get(0).getParents().size());

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId("SM1");
        req.setDeleteCourseUnits(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(0, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(0, courseUnits.get(0).getParents().size());
    }

    @Test
    public void testSendingDeleteStudyModuleMessageWithCourseUnits_shouldFail() throws JMSException {

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "ORG1", Collections.emptyList(), new LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "ORG1", Collections.emptyList(), new LocalisedString("test", "test", "test"));
        courseUnit.setParents(Collections.singletonList(
            new StudyElementReference("SM1", "ORG1", StudyElementType.STUDY_MODULE)));

        studyModuleRepository.create(studyModule);
        courseUnitRepository.create(courseUnit);

        List<CourseUnitEntity> courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(1, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(1, courseUnits.get(0).getParents().size());

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId("SM1");

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "ORG1");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(1, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(1, courseUnits.get(0).getParents().size());
    }

    @Test
    public void testSendingDeleteStudyModuleMessageWithMultipleCourseUnitReferences_shouldSucceed() throws JMSException {

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "TUNI", Collections.emptyList(), new LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "METROP", Collections.emptyList(), new LocalisedString("test", "test", "test"));

        List<StudyElementReference> references = new ArrayList<>();
        references.add(new StudyElementReference("SM1","TUNI", StudyElementType.STUDY_MODULE));
        references.add(new StudyElementReference("SM2", "METROP", StudyElementType.STUDY_MODULE));

        courseUnit.setParents(references);

        studyModuleRepository.create(studyModule);
        courseUnitRepository.create(courseUnit);

        List<CourseUnitEntity> courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(1, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(2, courseUnits.get(0).getParents().size());

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId("SM1");
        req.setDeleteCourseUnits(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        courseUnits = courseUnitService.findAll(Pageable.unpaged());

        assertEquals(0, studyModuleService.findAll(Pageable.unpaged()).size());
        assertEquals(1, courseUnits.size());
        assertEquals(1, courseUnits.get(0).getParents().size());

        assertEquals("SM2", courseUnits.get(0).getParents().get(0).getReferenceIdentifier());
        assertEquals("METROP", courseUnits.get(0).getParents().get(0).getReferenceOrganizer());
    }

    @Test
    public void testSendingDeleteStudyModuleMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity);

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId(studyModuleEntity.getStudyElementId());

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity entityShouldBeNull = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNull(entityShouldBeNull);

        List<StudyModuleEntity> historyEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
            "opintokokonaisuudet-history", StudyModuleEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        StudyModuleEntity historyEntity = historyEntities.get(0);
        assertEquals(studyModuleEntity.getStudyElementId(), historyEntity.getStudyElementId());
        assertEquals(studyModuleEntity.getStudyElementIdentifierCode(), historyEntity.getStudyElementIdentifierCode());
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getCreatedTime()), DateUtils.getFormatted(historyEntity.getCreatedTime()));
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getSendingTime()), DateUtils.getFormatted(historyEntity.getSendingTime()));
        assertEquals(1, historyEntity.getCooperationNetworks().size());
        assertEquals(studyModuleEntity.getCooperationNetworks().get(0).getId(), historyEntity.getCooperationNetworks().get(0).getId());
        assertEquals(studyModuleEntity.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(studyModuleEntity.getName().getValue("sv"), historyEntity.getName().getValue("sv"));
        assertEquals(studyModuleEntity.getName().getValue("en"), historyEntity.getName().getValue("en"));
    }

    @Test
    public void testSendingDeleteStudyModuleMessage_shouldSuccessRemovesCourseUnitRefAndCreatesHistories() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisation.getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleEntity = studyModuleRepository.create(studyModuleEntity);

        StudyElementReference refToRemove = DtoInitializer.getStudyElementReferenceForStudyModule(
            studyModuleEntity.getStudyElementId(), studyModuleEntity.getOrganizingOrganisationId());

        StudyElementReference refNotToRemove = DtoInitializer.getStudyElementReferenceForStudyModule(
            "ANOTHER-PARENT1", studyModuleEntity.getOrganizingOrganisationId());

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithParents("CU-1", "CODE-1", organisation.getOrganisationTkCode(),
            Collections.singletonList(network), new LocalisedString("Jakson nimi 1", "Jakson nimi 1 Englanniksi", null), Arrays.asList(refToRemove, refNotToRemove));

        courseUnitRepository.create(courseUnitEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity);

        DeleteStudyModuleRequest req = new DeleteStudyModuleRequest();
        req.setStudyElementId(studyModuleEntity.getStudyElementId());
        req.setDeleteCourseUnits(true);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity entityShouldBeNull = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNull(entityShouldBeNull);

        List<StudyModuleEntity> studyModuleHistoryEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
            "opintokokonaisuudet-history", StudyModuleEntity.class);
        assertNotNull(studyModuleHistoryEntities);
        assertEquals(1, studyModuleHistoryEntities.size());

        StudyModuleEntity historyEntity = studyModuleHistoryEntities.get(0);
        assertEquals(studyModuleEntity.getStudyElementId(), historyEntity.getStudyElementId());
        assertEquals(studyModuleEntity.getStudyElementIdentifierCode(), historyEntity.getStudyElementIdentifierCode());
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getCreatedTime()), DateUtils.getFormatted(historyEntity.getCreatedTime()));
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getSendingTime()), DateUtils.getFormatted(historyEntity.getSendingTime()));
        assertEquals(1, historyEntity.getCooperationNetworks().size());
        assertEquals(studyModuleEntity.getCooperationNetworks().get(0).getId(), historyEntity.getCooperationNetworks().get(0).getId());
        assertEquals(studyModuleEntity.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(studyModuleEntity.getName().getValue("sv"), historyEntity.getName().getValue("sv"));
        assertEquals(studyModuleEntity.getName().getValue("en"), historyEntity.getName().getValue("en"));

        CourseUnitEntity parentRemovedCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId())
            .orElse(null);
        assertNotNull(parentRemovedCourseUnit);
        assertEquals(1, parentRemovedCourseUnit.getParents().size());
        assertTrue(parentRemovedCourseUnit.getParents().stream().anyMatch(
            p -> p.getReferenceIdentifier().equals(refNotToRemove.getReferenceIdentifier())
                && p.getReferenceType() == StudyElementType.STUDY_MODULE));
        assertTrue(parentRemovedCourseUnit.getParents().stream().noneMatch(
            p -> p.getReferenceIdentifier().equals(refToRemove.getReferenceIdentifier())
                && p.getReferenceType() == StudyElementType.STUDY_MODULE));

        List<CourseUnitEntity> courseUnitHistoryEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
            "opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(courseUnitHistoryEntities);
        assertEquals(1, courseUnitHistoryEntities.size());

        CourseUnitEntity courseUnitHistoryEntity = courseUnitHistoryEntities.get(0);

        assertEquals(courseUnitEntity.getStudyElementId(), courseUnitHistoryEntity.getStudyElementId());
        assertEquals(courseUnitEntity.getStudyElementIdentifierCode(), courseUnitHistoryEntity.getStudyElementIdentifierCode());
        assertEquals(DateUtils.getFormatted(courseUnitEntity.getCreatedTime()), DateUtils.getFormatted(courseUnitHistoryEntity.getCreatedTime()));
        assertEquals(DateUtils.getFormatted(courseUnitEntity.getSendingTime()), DateUtils.getFormatted(courseUnitHistoryEntity.getSendingTime()));
        assertEquals(1, courseUnitHistoryEntity.getCooperationNetworks().size());
        assertEquals(courseUnitEntity.getCooperationNetworks().get(0).getId(), courseUnitHistoryEntity.getCooperationNetworks().get(0).getId());
        assertEquals(courseUnitEntity.getName().getValue("fi"), courseUnitHistoryEntity.getName().getValue("fi"));
        assertEquals(courseUnitEntity.getName().getValue("sv"), courseUnitHistoryEntity.getName().getValue("sv"));
        assertEquals(courseUnitEntity.getName().getValue("en"), courseUnitHistoryEntity.getName().getValue("en"));
        assertEquals(2, courseUnitHistoryEntity.getParents().size());
        assertTrue(courseUnitHistoryEntity.getParents().stream().anyMatch(
            p -> p.getReferenceIdentifier().equals(refNotToRemove.getReferenceIdentifier())
                && p.getReferenceType() == StudyElementType.STUDY_MODULE));
        assertTrue(courseUnitHistoryEntity.getParents().stream().anyMatch(
            p -> p.getReferenceIdentifier().equals(refToRemove.getReferenceIdentifier())
                && p.getReferenceType() == StudyElementType.STUDY_MODULE));
    }
}
