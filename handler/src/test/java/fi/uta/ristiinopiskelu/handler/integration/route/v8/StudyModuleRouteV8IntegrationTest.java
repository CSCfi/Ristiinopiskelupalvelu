package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Keyword;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyModule;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyModuleRequestDTO;
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
import fi.uta.ristiinopiskelu.handler.helper.HistoryHelper;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.DeleteStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.UpdateStudyModuleRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class StudyModuleRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitRouteV8IntegrationTest.class);

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
    private NetworkService networkService;

    @Autowired
    private OrganisationService organisationService;
    
    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ModelMapper modelMapper;

    private int messageSchemaVersion = 8;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI", "METROP", "LAUREA");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingCreateStudyModuleMessage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setTeachingLanguage(Collections.singletonList("EN"));
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithoutCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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

        studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity createdCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(1, createdCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdCourseUnitEntity.getStatus());

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
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), null,
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.emptyList(),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO subCourseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID/1", "RAI/RAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateCourseUnitRequestDTO subSubCourseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID/2", "RAI/RAI123",
                new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID/2", "RAI/RAI/STUDYMODULE2",
                new LocalisedString("Alikokonaisuuden nimi 2", "Alikokonaisuuden nimi 2 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subSubCourseUnit));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID/1", "RAI/RAI/STUDYMODULE",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork studyModuleNetwork = createdStudyModuleEntity.getCooperationNetworks().get(0);
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdCourseUnitEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdCourseUnitEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork subCourseUnitNetwork = createdCourseUnitEntity.getCooperationNetworks().get(0);
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork subStudyModuleNetwork = createdSubStudyModuleEntity.getCooperationNetworks().get(0);
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdSubSubCourseUnitEntity.getParents().get(0).getReferenceType());
        assertNotNull(createdSubSubCourseUnitEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubSubCourseUnitEntity.getStatus());

        // Verify cooperation network data was gathered from network index
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork subSubStudyModuleNetwork = createdSubSubCourseUnitEntity.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), subSubStudyModuleNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), subSubStudyModuleNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), subSubStudyModuleNetwork.getName().getValue("sv"));
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithSubStudyModule_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
                new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdParentStudyModule.getStatus());

        StudyModuleEntity createdSubStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModule);
        assertNotNull(createdSubStudyModule.getCreatedTime());
        assertEquals(1, createdSubStudyModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdParentStudyModule.getOrganizingOrganisationId(), createdSubStudyModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdSubStudyModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubStudyModule.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithAlreadyExistingSubStudyModule_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
            networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        // create the first studymodule
        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(subStudyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdParentStudyModule.getStatus());

        StudyModuleEntity createdSubStudyModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModule);
        assertNotNull(createdSubStudyModule.getCreatedTime());
        assertEquals(1, createdSubStudyModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdParentStudyModule.getOrganizingOrganisationId(), createdSubStudyModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdSubStudyModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubStudyModule.getStatus());
    }

    @Test
    public void testSendingCreateStudyModuleMessageWithSubStudyModuleThatHasMissingCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
                new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), null,
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
                new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.emptyList(),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", null, null, Collections.singletonList(network),
                Collections.singletonList(organisationReference), null, new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("SM-ID1", "RAIRAI",
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
        //$.studyModules[0].subElements: should be valid to one and only one schema, but 0 are valid
        //$.studyModules[0].subElements: array found, null expected
        //$.studyModules[0].subElements[0]: should be valid to one and only one schema, but 0 are valid
        //$.studyModules[0].subElements[0].name: is missing but it is required
        //$.studyModules[0].subElements[0]: should be valid to one and only one schema, but 0 are valid
        //$.studyModules[0].subElements[0].creditsMax: integer found, null expected
        //$.studyModules[0].subElements[0].amountValueMin: is missing but it is required
        //$.studyModules[0].subElements[0].amountValueMax: is missing but it is required
        //$.studyModules[0].subElements[0].optionality: is missing but it is required
        //$.studyModules[0].subElements[0].creditsMin: is missing but it is required
        //$.studyModules[0].subElements[0].optionality: is missing but it is required
        //$.studyModules[0].subElements[0].optionality: is missing but it is required
        //$.studyModules[0].subElements[0].type: does not have a value in the enumeration [STUDY_MODULE]
        //$.studyModules[0].subElements[0].name: is missing but it is required.
        assertEquals(14, response.getErrors().size());
    }

    @Test
    public void testSendingCreateStudyModuleMessage_shouldFailSubStudyModuleMissingRequiredFields() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", null,
                new LocalisedString("Opintojakson nimi 2", "Opintojakson nimi 2 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        //$.studyModules[0]: should be valid to one and only one schema, but 0 are valid
        //$.studyModules[0].creditsMax: integer found, null expected
        //$.studyModules[0].optionality: string found, null expected
        //$.studyModules[0].amountValueMin: is missing but it is required
        //$.studyModules[0].amountValueMax: is missing but it is required
        //$.studyModules[0].creditsMin: is missing but it is required
        //$.studyModules[0].creditsMax: integer found, but [null] is required
        //$.studyModules[0].optionality: does not have a value in the enumeration [ALL].
        assertEquals(8, response.getErrors().size());
    }
    
    @Test
    public void testSendingCreateStudyModuleMessage_shouldFailMissingRequiredFields() throws JMSException, IOException {
        Organisation organisation = DtoInitializerV8.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        //$.studyModules[0]: should be valid to one and only one schema, but 0 are valid
        //$.studyModules[0].creditsMin: number found, null expected
        //$.studyModules[0].optionality: string found, null expected
        //$.studyModules[0].amountValueMin: is missing but it is required
        //$.studyModules[0].amountValueMax: is missing but it is required
        //$.studyModules[0].creditsMax: is missing but it is required
        //$.studyModules[0].creditsMin: number found, but [null] is required
        //$.studyModules[0].optionality: does not have a value in the enumeration [ALL]
        //$.studyModules[0].cooperationNetworks: is missing but it is required.
        assertEquals(9, response.getErrors().size());
    }

    @Test
    public void testSendingDeleteStudyModuleMessage_shouldSuccess() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

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
    public void testSendingUpdateStudyModuleMessage_shouldSuccess() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference =
            DtoInitializer.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
                "{\n" +
                        "\t\"studyModule\": {\n" +
                        "\t\t\"studyElementId\": \"ID1\",\n" +
                        "\t\t\"status\": \"ARCHIVED\",\n" +
                        "\t\t\"name\": {\n" +
                        "\t\t\t\"values\": {\n" +
                        "\t\t\t\t\"fi\": \"Kokonaisuuden nimi 2\",\n" +
                        "\t\t\t\t\"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                        "\t\t\t\t\"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED, updatedEntity.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_noCooperationNetworkNameOrStatusGiven_shouldFillCooperationNetworkFields() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";
        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity.getId(),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("HEIHEI", null, null),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));
        studyModuleEntity.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertTrue(entity.getCooperationNetworks().stream()
                .allMatch(createdCn -> network.getId().equals(createdCn.getId())
                        && network.getName().getValue("fi").equals(createdCn.getName().getValue("fi"))
                        && network.getName().getValue("en").equals(createdCn.getName().getValue("en"))
                        && network.getName().getValue("sv").equals(createdCn.getName().getValue("sv"))));

        String updateJson =
                "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"cooperationNetworks\": [\n" +
                "            {\n" +
                "                \"id\": \"" + networkEntity.getId() +"\",\n" +
                "                \"enrollable\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());
        assertTrue(updatedEntity.getCooperationNetworks().stream()
                .allMatch(updatedCn -> networkEntity.getId().equals(updatedCn.getId())
                        && networkEntity.getName().getValue("fi").equals(updatedCn.getName().getValue("fi"))
                        && networkEntity.getName().getValue("en").equals(updatedCn.getName().getValue("en"))
                        && networkEntity.getName().getValue("sv").equals(updatedCn.getName().getValue("sv"))
                        ));
    }

    @Test
    public void testSendingUpdateStudyModuleMessageWithEmptyCooperationNetworks_shouldSuccess() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
                "{\n" +
                        "\t\"studyModule\": {\n" +
                        "\t\t\"studyElementId\": \"ID1\",\n" +
                        "\t\t\"status\": \"ARCHIVED\",\n" +
                        "\t\t\"name\": {\n" +
                        "\t\t\t\"values\": {\n" +
                        "\t\t\t\t\"fi\": \"Kokonaisuuden nimi 2\",\n" +
                        "\t\t\t\t\"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                        "\t\t\t\t\"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t},\n" +
                        "\t\t\"cooperationNetworks\": [\n" +
                        "\t\t\t]\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED, updatedEntity.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_teachingLanguageInAllCapsOrInvalid_shouldSuccess() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
            "{\n" +
                "\t\"studyModule\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"teachingLanguage\": [\n" +
                "\t\t\t\t\"EN\"\n" +
                "\t\t\t],\n" +
                "\t\t\"languagesOfCompletion\": [\n" +
                "\t\t\t\t\"SV\"\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("en", updatedEntity.getTeachingLanguage().get(0));
        assertEquals("sv", updatedEntity.getLanguagesOfCompletion().get(0));

        updateJson =
            "{\n" +
                "\t\"studyModule\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"teachingLanguage\": [\n" +
                "\t\t\t\t\"ENCVXBXCBV\"\n" +
                "\t\t\t],\n" +
                "\t\t\"languagesOfCompletion\": [\n" +
                "\t\t\t\t\"CASDDC\"\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(0, updatedEntity.getTeachingLanguage().size());
        assertEquals(0, updatedEntity.getLanguagesOfCompletion().size());
    }

    @Test
    public void testSendingDeleteStudyModuleMessageWithCourseUnits_shouldSuccess() throws JMSException {

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "TUNI",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "TUNI",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));
        courseUnit.setParents(Collections.singletonList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("SM1", "TUNI",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE)));

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

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "ORG1",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "ORG1",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));
        courseUnit.setParents(Collections.singletonList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("SM1", "ORG1",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE)));

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

        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity("SM1", "SM1_CODE1", "TUNI",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "CU1_CODE1", "METROP",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference> references = new ArrayList<>();
        references.add(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("SM1","TUNI",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));
        references.add(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("SM2", "METROP",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));

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
    public void testSendingUpdateStudyModuleMessage_shouldFailHasExtraField() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference =
            DtoInitializer.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
                "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"thisdoesnotexist\": \"randomcontent\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        assertNotNull(responseMessage);

        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingDeleteStudyModuleMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference =
            DtoInitializer.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference =
            DtoInitializer.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisation.getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleEntity = studyModuleRepository.create(studyModuleEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference refToRemove = DtoInitializer.getStudyElementReferenceForStudyModule(
                studyModuleEntity.getStudyElementId(), studyModuleEntity.getOrganizingOrganisationId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference refNotToRemove = DtoInitializer.getStudyElementReferenceForStudyModule(
                "ANOTHER-PARENT1", studyModuleEntity.getOrganizingOrganisationId());

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithParents("CU-1", "CODE-1", organisation.getOrganisationTkCode(),
                Collections.singletonList(network),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Jakson nimi 1", "Jakson nimi 1 Englanniksi", null),
            Arrays.asList(refToRemove, refNotToRemove));

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
                    && p.getReferenceType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));
        assertTrue(parentRemovedCourseUnit.getParents().stream().noneMatch(
                p -> p.getReferenceIdentifier().equals(refToRemove.getReferenceIdentifier())
                        && p.getReferenceType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));

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
                        && p.getReferenceType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));
        assertTrue(courseUnitHistoryEntity.getParents().stream().anyMatch(
                p -> p.getReferenceIdentifier().equals(refToRemove.getReferenceIdentifier())
                        && p.getReferenceType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE));
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference =
            DtoInitializer.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
                "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
                "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\",\n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
                organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));

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

        String updateJson2 =
                "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"Kokonaisuuden nimi 3\",\n" +
                "                \"en\": \"Kokonaisuuden nimi 3 Englanniksi\",\n" +
                "                \"sv\": \"Kokonaisuuden nimi 3 Ruotsiksi\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson2, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
                organisationReference.getOrganisation().getOrganisationTkCode());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity2 = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity2);
        assertNotNull(updatedEntity2.getCreatedTime());
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getCreatedTime()), DateUtils.getFormatted(updatedEntity2.getCreatedTime()));
        assertNotNull(updatedEntity2.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 3", updatedEntity2.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 3 Englanniksi", updatedEntity2.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 3 Ruotsiksi", updatedEntity2.getName().getValue("sv"));

        studyModuleHistoryEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
                "opintokokonaisuudet-history", StudyModuleEntity.class);
        assertNotNull(studyModuleHistoryEntities);
        assertEquals(2, studyModuleHistoryEntities.size());

        StudyModuleEntity historyEntity2 = studyModuleHistoryEntities.stream()
                .sorted(Comparator.comparing(StudyModuleEntity::getUpdateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .findFirst()
                .orElse(null);
        assertEquals(updatedEntity.getStudyElementId(), historyEntity2.getStudyElementId());
        assertEquals(updatedEntity.getStudyElementIdentifierCode(), historyEntity2.getStudyElementIdentifierCode());
        assertEquals(updatedEntity.getCreatedTime(), historyEntity2.getCreatedTime());
        assertEquals(updatedEntity.getSendingTime(), historyEntity2.getSendingTime());
        assertEquals(updatedEntity.getUpdateTime(), historyEntity2.getUpdateTime());
        assertEquals(1, historyEntity2.getCooperationNetworks().size());
        assertEquals(updatedEntity.getCooperationNetworks().get(0).getId(), historyEntity2.getCooperationNetworks().get(0).getId());
        assertEquals(updatedEntity.getName().getValue("fi"), historyEntity2.getName().getValue("fi"));
        assertEquals(updatedEntity.getName().getValue("sv"), historyEntity2.getName().getValue("sv"));
        assertEquals(updatedEntity.getName().getValue("en"), historyEntity2.getName().getValue("en"));
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingCourseUnitAndRemoveCurrentReference() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new course unit to be added to study module
        CourseUnitEntity courseUnitEntityToAddRefs = EntityInitializer.getCourseUnitEntity("CU-2", "CU-CODE2",
                organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OJ 2", null, null));

        courseUnitRepository.create(courseUnitEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
                "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
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

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify one course unit is found with parent reference to parent study module
        List<CourseUnitEntity> courseUnitEntities = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);

        assertNotNull(courseUnitEntities);
        assertTrue(courseUnitEntities.size() == 1);
        assertEquals(courseUnitEntityToAddRefs.getStudyElementId(), courseUnitEntities.get(0).getStudyElementId());
        assertEquals(courseUnitEntityToAddRefs.getStudyElementIdentifierCode(), courseUnitEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(courseUnitEntityToAddRefs.getOrganizingOrganisationId(), courseUnitEntities.get(0).getOrganizingOrganisationId());

        // Verify existing reference to course unit is removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldSuccessAndNotMultiplyParentRefs() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO subCourseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModuleRefToBeRemoved = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID3", "RAIRAI3",
                new LocalisedString("Alikokonaisuus", "Alikokonaisuus 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
                new LocalisedString("Alikokonaisuus", "Alikokonaisuus 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5),
                Arrays.asList(subStudyModule, subCourseUnit, subStudyModuleRefToBeRemoved));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // Verify course unit created and has parent reference
        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                subCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Verify course unit created and has parent reference
        StudyModuleEntity studyModuleEntityCreatedWithModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleEntityCreatedWithModule);
        assertEquals(1, studyModuleEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, studyModuleEntityCreatedWithModule.getStatus());

        // Verify course unit created and has parent reference
        StudyModuleEntity studyModuleRefToBeRemoved = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModuleRefToBeRemoved.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleRefToBeRemoved);
        assertEquals(1, studyModuleRefToBeRemoved.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleRefToBeRemoved.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleRefToBeRemoved.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleRefToBeRemoved.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, studyModuleRefToBeRemoved.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
                organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OK 2", null, null));

        studyModuleRepository.create(studyModuleEntityToAddRefs);

        // Send update study module message with same sub study elements and one new
        String updateJson =
                "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
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
                "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"studyElementIdentifierCode\": \""+ courseUnitEntityCreatedWithModule.getStudyElementIdentifierCode() + "\",\n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
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
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"studyElementIdentifierCode\": \""+ studyModuleEntityToAddRefs.getStudyElementIdentifierCode() + "\",\n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
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

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify parent reference is not duplicated
        studyModuleEntityCreatedWithModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleEntityCreatedWithModule);
        assertEquals(1, studyModuleEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, studyModuleEntityCreatedWithModule.getStatus());

        // Verify parent reference is not duplicated
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                subCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Verify parent reference is added
        StudyModuleEntity studyModuleRefsAdded = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntityToAddRefs.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode())
                .orElse(null);
        assertNotNull(studyModuleRefsAdded);
        assertEquals(1, studyModuleRefsAdded.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleRefsAdded.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleRefsAdded.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleRefsAdded.getParents().get(0).getReferenceType());

        // Verify study module parent reference was removed
        studyModuleRefToBeRemoved = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModuleRefToBeRemoved.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleRefToBeRemoved);
        assertEquals(0, studyModuleRefToBeRemoved.getParents().size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, studyModuleRefToBeRemoved.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingStudyModule() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
                organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OK 2", null, null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
                "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
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
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
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

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 1);
        assertEquals(studyModuleEntityToAddRefs.getStudyElementId(), studyModuleEntities.get(0).getStudyElementId());
        assertEquals(studyModuleEntityToAddRefs.getStudyElementIdentifierCode(), studyModuleEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(studyModuleEntityToAddRefs.getOrganizingOrganisationId(), studyModuleEntities.get(0).getOrganizingOrganisationId());

        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingCourseUnitAndRemoveCurrentReferenceContainsForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU/ID1", "JEP/JEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID/1", "RAI/RAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new course unit to be added to study module
        CourseUnitEntity courseUnitEntityToAddRefs = EntityInitializer.getCourseUnitEntity("CU/2", "CU/CODE2",
                organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OJ 2", null, null));

        courseUnitRepository.create(courseUnitEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
                        "{\n" +
                        "    \"studyModule\": { \n" +
                        "        \"studyElementId\": \"" + studyModule.getStudyElementId() + "\", \n" +
                        "        \"name\": { \n" +
                        "            \"values\": { \n" +
                        "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                        "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                        "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                        "            } \n" +
                        "        }, \n" +
                        "        \"subElements\": [\n" +
                        "            {\n" +
                        "                \"studyElementId\": \""+ courseUnitEntityToAddRefs.getStudyElementId() + "\", \n" +
                        "                \"type\": \"COURSE_UNIT\",\n" +
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

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify one course unit is found with parent reference to parent study module
        List<CourseUnitEntity> courseUnitEntities = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);

        assertNotNull(courseUnitEntities);
        assertTrue(courseUnitEntities.size() == 1);
        assertEquals(courseUnitEntityToAddRefs.getStudyElementId(), courseUnitEntities.get(0).getStudyElementId());
        assertEquals(courseUnitEntityToAddRefs.getStudyElementIdentifierCode(), courseUnitEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(courseUnitEntityToAddRefs.getOrganizingOrganisationId(), courseUnitEntities.get(0).getOrganizingOrganisationId());

        // Verify existing reference to course unit is removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailSubElementRefNotFound() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // SEND UPDATE STUDY MODULE MESSAGE
        String updateJson =
                "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \"NOTFOUND_ID\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"NOT_FOUND\"\n" +
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
        assertTrue(updResp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldRemoveAllReferencesWithEmptySubElements() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID123", "SUBSTUDYMODULE-1",
                new LocalisedString("Alikokonaisuuden nimi 1", "Alikokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Arrays.asList(courseUnit, subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        StudyModuleEntity createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(1, createdSubStudyModuleEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Send update study module to remove all sub elements
        String updateJson =
                "{\n" +
                        "    \"studyModule\": { \n" +
                        "        \"studyElementId\": \"ID1\", \n" +
                        "        \"name\": { \n" +
                        "            \"values\": { \n" +
                        "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                        "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                        "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                        "            } \n" +
                        "        }, \n" +
                        "        \"subElements\": []\n" +
                        "    }\n" +
                        "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
                organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntitiesWithRef = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntitiesWithRef);
        assertTrue(studyModuleEntitiesWithRef.size() == 0);

        List<CourseUnitEntity> courseUnitEntitiesWithRef = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);;
        assertNotNull(courseUnitEntitiesWithRef);
        assertTrue(courseUnitEntitiesWithRef.size() == 0);

        // Double check references have been removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertNotNull(courseUnitEntityCreatedWithModule.getUpdateTime());
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());

        createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(0, createdSubStudyModuleEntity.getParents().size());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldRemoveAllReferencesWithNullSubElements() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO subStudyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID123", "SUBSTUDYMODULE-1",
                new LocalisedString("Alikokonaisuuden nimi 1", "Alikokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Arrays.asList(courseUnit, subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        StudyModuleEntity createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(1, createdSubStudyModuleEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Send update study module to remove all sub elements
        String updateJson =
                "{\n" +
                        "    \"studyModule\": { \n" +
                        "        \"studyElementId\": \"ID1\", \n" +
                        "        \"name\": { \n" +
                        "            \"values\": { \n" +
                        "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                        "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                        "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                        "            } \n" +
                        "        }, \n" +
                        "        \"subElements\": null\n" +
                        "    }\n" +
                        "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
                organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntitiesWithRef = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntitiesWithRef);
        assertTrue(studyModuleEntitiesWithRef.size() == 0);

        List<CourseUnitEntity> courseUnitEntitiesWithRef = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);;
        assertNotNull(courseUnitEntitiesWithRef);
        assertTrue(courseUnitEntitiesWithRef.size() == 0);

        // Double check references have been removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertNotNull(courseUnitEntityCreatedWithModule.getUpdateTime());
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());

        createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(0, createdSubStudyModuleEntity.getParents().size());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingStudyModuleButNotUpdateSubElementFields() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity(
                "OK-2", "OK-CODE2", organisationReference.getOrganisation().getOrganisationTkCode(),
                Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OK 2", "OK 2 en", null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
                "{\n" +
                        "    \"studyModule\": { \n" +
                        "        \"studyElementId\": \"ID1\", \n" +
                        "        \"name\": { \n" +
                        "            \"values\": { \n" +
                        "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                        "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                        "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                        "            } \n" +
                        "        }, \n" +
                        "        \"subElements\": [\n" +
                        "            {\n" +
                        "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                        "                \"type\": \"COURSE_UNIT\",\n" +
                        "                \"organisationReferences\": [\n" +
                        "                   {\n" +
                        "                       \"organisationRole\": 1,\n" +
                        "                       \"target\": {\n" +
                        "                           \"organisationTkCode\": \"TUNI\"\n" +
                        "                       }\n" +
                        "                   }\n" +
                        "                ],\n" +
                        "                \"name\": { \n" +
                        "                    \"values\": { \n" +
                        "                        \"fi\": \"Opintojakson nimi 2\", \n" +
                        "                        \"en\": \"Opintojakson nimi 2 Englanniksi\", \n" +
                        "                        \"sv\": \"Opintojakson nimi 2 Ruotsiksi\" \n" +
                        "                    } \n" +
                        "               }\n" +
                        "            },\n" +
                        "            {\n" +
                        "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                        "                \"type\": \"STUDY_MODULE\",\n" +
                        "                \"organisationReferences\": [\n" +
                        "                   {\n" +
                        "                       \"organisationRole\": 1,\n" +
                        "                       \"target\": {\n" +
                        "                           \"organisationTkCode\": \"TUNI\"\n" +
                        "                       }\n" +
                        "                   }\n" +
                        "                ],\n" +
                        "                \"name\": { \n" +
                        "                    \"values\": { \n" +
                        "                        \"fi\": \"OK 3\", \n" +
                        "                        \"en\": \"OK 3 en\", \n" +
                        "                        \"sv\": \"OK 3 sv\" \n" +
                        "                    } \n" +
                        "               }\n" +
                        "            }\n" +
                        "        ]\n" +
                        "    }\n" +
                        "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
                organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 1);
        assertEquals(studyModuleEntityToAddRefs.getStudyElementId(), studyModuleEntities.get(0).getStudyElementId());
        assertEquals(studyModuleEntityToAddRefs.getStudyElementIdentifierCode(), studyModuleEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(studyModuleEntityToAddRefs.getOrganizingOrganisationId(), studyModuleEntities.get(0).getOrganizingOrganisationId());
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("fi"), studyModuleEntities.get(0).getName().getValue("fi"));
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("en"), studyModuleEntities.get(0).getName().getValue("en"));
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("sv"), studyModuleEntities.get(0).getName().getValue("sv"));

        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
        assertEquals(courseUnit.getName().getValue("fi"), courseUnitEntityCreatedWithModule.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("en"), courseUnitEntityCreatedWithModule.getName().getValue("en"));
        assertEquals(courseUnit.getName().getValue("sv"), courseUnitEntityCreatedWithModule.getName().getValue("sv"));
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailAndNotUpdateParentAndAddReferencesToExistingStudyModule() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
                organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("OK 2", null, null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
                "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"EXTRAFIELD\": \"ASDASD\", \n" +
                "        \"UNKNOWNFIELD\": \"ASDASD\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
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
        assertTrue(updResp.getStatus() == Status.FAILED);

        // Verify created study module exists but is not updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNull(updatedEntity.getUpdateTime());
        assertEquals(studyModule.getName().getValue("fi"), updatedEntity.getName().getValue("fi"));
        assertEquals(studyModule.getName().getValue("en"), updatedEntity.getName().getValue("en"));
        assertEquals(studyModule.getName().getValue("sv"), updatedEntity.getName().getValue("sv"));
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify no references added
        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
                organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 0);

        // Verify reference not removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldAllowCourseUnitReferencesFromDifferentOrganisationInSameNetwork() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(studyModuleOrganisingOrganisationId, courseUnitOrganisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializerV8.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializerV8.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializerV8.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializerV8.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
                Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);
        
        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);
        
        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModule.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertNotNull(courseUnitEntity.getParents());
        assertEquals(1, courseUnitEntity.getParents().size());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference studyModuleReference = courseUnitEntity.getParents().get(0);

        assertEquals(studyModule.getStudyElementId(), studyModuleReference.getReferenceIdentifier());
        assertEquals(studyModuleOrganisation.getOrganisationTkCode(), studyModuleReference.getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleReference.getReferenceType());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_cooperationNetworksEmpty_shouldSucceed() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(studyModuleOrganisingOrganisationId, courseUnitOrganisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
            networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializerV8.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializerV8.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializerV8.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializerV8.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);

        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);

        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModule.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // METROP updates the studymodule's cooperationNetworks to empty
        studyModule.setCooperationNetworks(Collections.emptyList());
        courseUnit.setCooperationNetworks(Collections.emptyList());

        studyModule.setSubElements(Collections.singletonList(courseUnit));
        updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModule.class));

        studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertNotNull(courseUnitEntity.getParents());
        assertEquals(1, courseUnitEntity.getParents().size());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference studyModuleReference = courseUnitEntity.getParents().get(0);

        assertEquals(studyModule.getStudyElementId(), studyModuleReference.getReferenceIdentifier());
        assertEquals(studyModuleOrganisation.getOrganisationTkCode(), studyModuleReference.getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, studyModuleReference.getReferenceType());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailCourseUnitReferencesFromDifferentOrganisationInDifferentNetwork() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity studyModuleNetworkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(studyModuleOrganisingOrganisationId));
        CooperationNetwork studyModuleNetwork = DtoInitializerV8.getCooperationNetwork(
                studyModuleNetworkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        NetworkEntity courseUnitNetworkEntity = persistNetworkEntity("CN-2",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 2", "Verkosto en", "Verkosto sv"), Arrays.asList(courseUnitOrganisingOrganisationId));
        CooperationNetwork courseUnitNetwork = DtoInitializerV8.getCooperationNetwork(
                courseUnitNetworkEntity.getId(), new LocalisedString("Verkosto 2", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializerV8.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializerV8.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializerV8.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializerV8.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
                new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(studyModuleNetwork),
                Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
                new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(courseUnitNetwork),
                Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);

        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);

        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModule.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.FAILED);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertTrue(CollectionUtils.isEmpty(courseUnitEntity.getParents()));
    }

    @Test
    public void testSendingCreateStudyModuleMessage_addOnlyEnrollableWithParentCourseUnit_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
            networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));
        courseUnit.setOnlyEnrollableWithParent(true);
        courseUnit.setGroupSize(123);

        CreateStudyModuleRequestDTO studyModule = DtoInitializerV8.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, createdCourseUnitEntity.getParents().get(0).getReferenceType());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork networkCurrentVersion = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("ID2", "RAIRAI2", "TUNI",
            Collections.singletonList(networkCurrentVersion),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakson nimi 2", "Course unit to add ref to", null));

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, refOnlyEnrollableRemovedCourseUnitEntity.getParents().get(0).getReferenceType());


        CourseUnitEntity refAddedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit2.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdCourseUnitEntity);
        assertEquals(1, refAddedCourseUnitEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), refAddedCourseUnitEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(true, refAddedCourseUnitEntity.getParents().get(0).getOnlyEnrollableWithParent());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), refAddedCourseUnitEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE, refAddedCourseUnitEntity.getParents().get(0).getReferenceType());
    }

    // TODO: move to a separate service layer integration test someday?
    @Test
    public void testCreatingDuplicateStudyModules_shouldFail() {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation org = DtoInitializer.getOrganisation("TESTORG", "TESTORG");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org,
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("TESTORG", "blaa",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", null, null), 8);
        organisationService.create(organisationEntity);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity("TESTID", "TESTORG", null, null, null);
        studyModuleEntity.setOrganisationReferences(Collections.singletonList(organisationRef));

        studyModuleService.create(studyModuleEntity);
        studyModuleEntity.setId(null);
        assertThrows(CreateFailedException.class, () -> studyModuleService.create(studyModuleEntity));
    }

    private NetworkEntity persistNetworkEntity(String id, fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString name, List<String> organisationIds) {
        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation> networkOrgs = new ArrayList<>();
        for (String orgId : organisationIds) {
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation networkOrganisation =
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation();
            networkOrganisation.setOrganisationTkCode(orgId);
            networkOrganisation.setValidityInNetwork(DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now()));
            networkOrganisation.setIsCoordinator(true);
            networkOrgs.add(networkOrganisation);
        }
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(id, name, networkOrgs,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        return networkRepository.create(networkEntity);
    }
}
