package fi.uta.ristiinopiskelu.handler.integration.route.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializerV8;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.CreateStudyModuleRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class CreateStudyModuleRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateStudyModuleRouteIntegrationTest.class);

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
    private ModelMapper modelMapper;

    @Value("${general.messageSchema.version}")
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
    public void testSendingCreateStudyModuleMessage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ARCHIVED);
        studyModule.setCredits(BigDecimal.valueOf(10));

        Keyword keyword = new Keyword();
        keyword.setKey("testkey");
        keyword.setKeySet("testKeySet");
        keyword.setValue(new LocalisedString("value fi", "value en", "value sv"));
        studyModule.setKeywords(Collections.singletonList(keyword));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<StudyModuleEntity> savedStudyModules = StreamSupport.stream(studyModuleRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedStudyModules != null);
        assertEquals(1, savedStudyModules.size());

        StudyModuleEntity result = savedStudyModules.get(0);
        assertEquals(studyModule.getStudyElementId(), result.getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertEquals(StudyStatus.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithoutCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.emptyList(),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ARCHIVED);

        Keyword keyword = new Keyword();
        keyword.setKey("testkey");
        keyword.setKeySet("testKeySet");
        keyword.setValue(new LocalisedString("value fi", "value en", "value sv"));
        studyModule.setKeywords(Collections.singletonList(keyword));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), null,
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ARCHIVED);
        studyModule.setKeywords(Collections.singletonList(keyword));

        req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithCourseUnit_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity createdCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(1, createdCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdCourseUnitEntity.getStatus());

        List<StudyModuleEntity> savedStudyModules = StreamSupport.stream(studyModuleRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedStudyModules != null);
        assertEquals(1, savedStudyModules.size());

        StudyModuleEntity result = savedStudyModules.get(0);
        assertNotNull(result.getCreatedTime());
        assertEquals(studyModule.getStudyElementId(), result.getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithCourseUnitThatHasMissingCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), null,
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.emptyList(),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithCourseUnitAndSubStudyModule_shouldSucceedContainsForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO subCourseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID/1", "RAI/RAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CourseUnitWriteDTO subSubCourseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID/2", "RAI/RAI123",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID/2", "RAI/RAI/STUDYMODULE2",
            new LocalisedString("Alikokonaisuuden nimi 2", "Alikokonaisuuden nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subSubCourseUnit));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID/1", "RAI/RAI/STUDYMODULE",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Arrays.asList(subStudyModule, subCourseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify parent studymodule created
        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        CooperationNetwork studyModuleNetwork = createdStudyModuleEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), studyModuleNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), studyModuleNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), studyModuleNetwork.getName().getValue("sv"));

        // Verify sub course unit created
        CourseUnitEntity createdCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            subCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(1, createdCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdCourseUnitEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        CooperationNetwork subCourseUnitNetwork = createdCourseUnitEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), subCourseUnitNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), subCourseUnitNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), subCourseUnitNetwork.getName().getValue("sv"));

        // Verify sub studyModule created
        StudyModuleEntity createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(1, createdSubStudyModuleEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        CooperationNetwork subStudyModuleNetwork = createdSubStudyModuleEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), subStudyModuleNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), subStudyModuleNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), subStudyModuleNetwork.getName().getValue("sv"));

        // Verify sub sub course unit created
        CourseUnitEntity createdSubSubCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            subSubCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubSubCourseUnitEntity);
        assertNotNull(createdSubSubCourseUnitEntity.getCreatedTime());
        assertEquals(1, createdSubSubCourseUnitEntity.getParents().size());
        assertEquals(createdSubStudyModuleEntity.getStudyElementId(), createdSubSubCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdSubStudyModuleEntity.getOrganizingOrganisationId(), createdSubSubCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdSubSubCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdSubSubCourseUnitEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdSubSubCourseUnitEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        CooperationNetwork subSubStudyModuleNetwork = createdSubSubCourseUnitEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), subSubStudyModuleNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), subSubStudyModuleNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), subSubStudyModuleNetwork.getName().getValue("sv"));
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithSubStudyModule_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<StudyModuleEntity> savedStudyModules = StreamSupport.stream(studyModuleRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedStudyModules != null);
        assertEquals(2, savedStudyModules.size());

        List<StudyModuleEntity> result = savedStudyModules.stream().sorted(Comparator.comparing(StudyModuleEntity::getStudyElementId)).collect(Collectors.toList());
        assertEquals(studyModule.getStudyElementId(), result.get(0).getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), result.get(0).getStudyElementIdentifierCode());
        assertNotNull(result.get(0).getCreatedTime());

        assertEquals(subStudyModule.getStudyElementId(), result.get(1).getStudyElementId());
        assertEquals(subStudyModule.getStudyElementIdentifierCode(), result.get(1).getStudyElementIdentifierCode());

        StudyModuleEntity createdParentStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdParentStudyModule);
        assertNotNull(createdParentStudyModule.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdParentStudyModule.getStatus());

        StudyModuleEntity createdSubStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModule);
        assertNotNull(createdSubStudyModule.getCreatedTime());
        assertEquals(1, createdSubStudyModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdParentStudyModule.getOrganizingOrganisationId(), createdSubStudyModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdSubStudyModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, createdSubStudyModule.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithAlreadyExistingSubStudyModule_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        // create the first studymodule
        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(subStudyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subStudyModule));

        // now create the second studymodule. this should succeed since creating a studymodule with an already existing studymodule as a subelement should just
        // update the references of the already existing studymodule, not fail.
        req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<StudyModuleEntity> savedStudyModules = StreamSupport.stream(studyModuleRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertTrue(savedStudyModules != null);
        assertEquals(2, savedStudyModules.size());

        List<StudyModuleEntity> result = savedStudyModules.stream().sorted(Comparator.comparing(StudyModuleEntity::getStudyElementId)).collect(Collectors.toList());
        assertEquals(studyModule.getStudyElementId(), result.get(0).getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), result.get(0).getStudyElementIdentifierCode());
        assertNotNull(result.get(0).getCreatedTime());

        assertEquals(subStudyModule.getStudyElementId(), result.get(1).getStudyElementId());
        assertEquals(subStudyModule.getStudyElementIdentifierCode(), result.get(1).getStudyElementIdentifierCode());

        StudyModuleEntity createdParentStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdParentStudyModule);
        assertNotNull(createdParentStudyModule.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdParentStudyModule.getStatus());

        StudyModuleEntity createdSubStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModule);
        assertNotNull(createdSubStudyModule.getCreatedTime());
        assertEquals(1, createdSubStudyModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdParentStudyModule.getOrganizingOrganisationId(), createdSubStudyModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdSubStudyModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, createdSubStudyModule.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithSubStudyModuleThatHasMissingCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), null,
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.emptyList(),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subStudyModule));

        req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyModuleMessage_subCourseUnitMissingName_shouldFailNameRequired() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", null, null, Collections.singletonList(network),
            Collections.singletonList(organisationReference), null, new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("SM-ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);

        // Errors that should be given:
        // $.studyModules[0].subElements: array found, null expected - array found, null expected - does not match other "oneOf" option subElement (null)
        // $.studyModules[0].subElements[0].creditsMax: integer found, null expected - Tested against sub study module schemas optionality
        // $.studyModules[0].subElements[0].type: does not have a value in the enumeration [STUDY_MODULE] - does not have a value in the enumeration [STUDY_MODULE]
        assertEquals(3, response.getErrors().size());
    }

    @Test
    public void testSendingCreateStudyModuleMessage_shouldFailSubStudyModuleMissingRequiredFields() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", null,
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), null, new BigDecimal(5), Collections.singletonList(subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);

        // Errors that should be given:
        // $.studyModules[0].creditsMax: integer found, null expected
        // $.studyModules[0].optionality: string found, null expected
        // $.studyModules[0].optionality: does not have a value in the enumeration [ALL]
        // $.studyModules[0].creditsMax: integer found, but [null] is required.
        assertEquals(4, response.getErrors().size());
    }

    @Test
    public void testSendingCreateStudyModuleMessage_shouldFailMissingRequiredFields() throws JMSException, IOException {
        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), null,
            Collections.singletonList(organisationReference), new BigDecimal(2.5), null, null);

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        Object resp = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp instanceof JsonValidationFailedResponse);

        JsonValidationFailedResponse response = (JsonValidationFailedResponse) resp;
        assertTrue(response.getStatus() == Status.FAILED);

        // Errors that should be given:
        // $.studyModules[0].creditsMin: number found, null expected
        // $.studyModules[0].optionality: string found, null expected
        // $.studyModules[0].optionality: does not have a value in the enumeration [ALL]
        // $.studyModules[0].creditsMin: number found, but [null] is required
        // $.studyModules[0].cooperationNetworks: is missing but it is required
        assertEquals(5, response.getErrors().size());
    }

    @Test
    public void testSendingCreateStudyModuleMessage_addOnlyEnrollableWithParentCourseUnit_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));
        courseUnit.setOnlyEnrollableWithParent(true);
        courseUnit.setGroupSize(123);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);

        CourseUnitEntity createdCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertEquals(1, createdCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(courseUnit.getOnlyEnrollableWithParent(), createdCourseUnitEntity.getParents().get(0).getOnlyEnrollableWithParent());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("ID2", "RAIRAI2", "TUNI",
            Collections.singletonList(network), new LocalisedString("Opintojakson nimi 2", "Course unit to add ref to", null));

        courseUnitRepository.create(courseUnit2);

        // Test updating new reference to study module with onlyEnrollableWithParent and remove requirement from previously created
        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnit.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"onlyEnrollableWithParent\": false,\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnit2.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"onlyEnrollableWithParent\": true,\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        CourseUnitEntity refOnlyEnrollableRemovedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertEquals(1, refOnlyEnrollableRemovedCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), refOnlyEnrollableRemovedCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(false, refOnlyEnrollableRemovedCourseUnitEntity.getParents().get(0).getOnlyEnrollableWithParent());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), refOnlyEnrollableRemovedCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, refOnlyEnrollableRemovedCourseUnitEntity.getParents().get(0).getReferenceType());


        CourseUnitEntity refAddedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit2.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertEquals(1, refAddedCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), refAddedCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(true, refAddedCourseUnitEntity.getParents().get(0).getOnlyEnrollableWithParent());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), refAddedCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, refAddedCourseUnitEntity.getParents().get(0).getReferenceType());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithInvalidLanguage_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString studyModuleName = new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden imi 1 Englanniksi", null);

        // first with valid language
        StudyModuleWriteDTO studyModuleRequest = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            studyModuleName, Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0),
            null);
        studyModuleRequest.setStudyElementPermanentId("PERMID1");
        studyModuleRequest.setStatus(StudyStatus.CANCELLED);
        studyModuleRequest.setTeachingLanguage(Collections.singletonList("fi"));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModuleRequest));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        LocalisedString studyModule2Name = new LocalisedString("Kokonaisuuden nimi 2", "Kokonaisuuden nimi 2 Englanniksi", null);

        // then with bogus language
        StudyModuleWriteDTO studyModuleRequest2 = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI",
            studyModule2Name, Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0),
            null);
        studyModuleRequest2.setStudyElementPermanentId("PERMID2");
        studyModuleRequest2.setStatus(StudyStatus.ACTIVE);
        studyModuleRequest2.setTeachingLanguage(Collections.singletonList("raipatirai"));

        req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModuleRequest2));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse jsonValidationFailedResponse = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(jsonValidationFailedResponse.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyModuleMessageV8WithInvalidLanguage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        // test v8 api. teaching language should be set to empty because no valid language was given
        fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation organisationV8 = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference orgRefV8 = DtoInitializerV8.getOrganisationReference(organisationV8,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole.ROLE_MAIN_ORGANIZER);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyModuleRequestDTO studyModuleV8 = DtoInitializerV8.getCreateStudyModuleRequestDTO(
            "ID3", "RAIRAI", new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("test", null, null),
            Collections.singletonList(DtoInitializerV8.getCooperationNetwork(networkEntity.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))),
            Collections.singletonList(orgRefV8), new BigDecimal(2.5), new BigDecimal(5.0), null);
        studyModuleV8.setTeachingLanguage(Collections.singletonList("pöö"));

        fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest studyModuleRequestV8 =
            new fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest();
        studyModuleRequestV8.setStudyModules(Collections.singletonList(studyModuleV8));

        JmsHelper.setMessageSchemaVersion(8);
        OrganisationEntity organisationEntity = organisationService.findById("TUNI").get();
        organisationEntity.setSchemaVersion(8);
        organisationService.update(organisationEntity);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, studyModuleRequestV8, organisingOrganisationId);

        fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse v8response =
            (fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(v8response.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);

        StudyModuleEntity created = studyModuleRepository.findByStudyElementIdAndOrganizingOrganisationId("ID3", "TUNI").get();
        assertEquals(Collections.emptyList(), created.getTeachingLanguage());
    }

    // TODO: move to a separate service layer integration test someday?
    @Test
    public void testCreatingDuplicateStudyModules_shouldFail() {
        Organisation org = DtoInitializer.getOrganisation("TESTORG", "TESTORG");
        OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("TESTORG", "blaa", new LocalisedString("test", null, null), 8);
        organisationService.create(organisationEntity);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity("TESTID", "TESTORG", null, null, null);
        studyModuleEntity.setOrganisationReferences(Collections.singletonList(organisationRef));

        studyModuleService.create(studyModuleEntity);
        studyModuleEntity.setId(null);
        assertThrows(CreateFailedException.class, () -> studyModuleService.create(studyModuleEntity));
    }
}
