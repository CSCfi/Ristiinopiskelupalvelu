package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.AssessmentItem;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CompletionOption;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CompletionOptionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializerV8;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.HistoryHelper;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.DeleteCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class CourseUnitRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitRouteV8IntegrationTest.class);

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
    private NetworkRepository networkRepository;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private MessageSchemaService schemaService;

    private int messageSchemaVersion = 8;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisation.setSchemaVersion(this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingCreateCourseUnitMessage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
                Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode()).get();
        assertNotNull(createdCourseUnit);
        assertNotNull(createdCourseUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, createdCourseUnit.getType());
        assertEquals(courseUnit.getStudyElementId(), createdCourseUnit.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), createdCourseUnit.getStudyElementIdentifierCode());
        assertEquals(courseUnit.getStudyElementPermanentId(), createdCourseUnit.getStudyElementPermanentId());
        assertEquals(1, createdCourseUnit.getCooperationNetworks().size());

        // Verify cooperation network data was gathered from network index
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork courseUnitNetwork = createdCourseUnit.getCooperationNetworks().get(0);
        assertEquals(networkEntity.getName().getValue("fi"), courseUnitNetwork.getName().getValue("fi"));
        assertEquals(networkEntity.getName().getValue("en"), courseUnitNetwork.getName().getValue("en"));
        assertEquals(networkEntity.getName().getValue("sv"), courseUnitNetwork.getName().getValue("sv"));

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(result.getStudyElementId(), result.getStudyElementId());
        assertEquals(result.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.CANCELLED, result.getStatus());
        assertNotEquals(result.getId(), courseUnit.getStudyElementId());
        assertNotEquals(result.getId(), courseUnit.getStudyElementIdentifierCode());
        assertNotEquals(result.getId(), courseUnit.getStudyElementPermanentId());
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertEquals(courseUnit.getStudyElementPermanentId(), result.getStudyElementPermanentId());
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithoutCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
                Collections.emptyList(), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getErrors().stream().anyMatch( e -> e.contains(".courseUnits[0].cooperationNetworks: there must be a minimum of 1 items in the array")));

        courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
                null, Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getErrors().stream().anyMatch(e -> e.contains(".courseUnits[0].cooperationNetworks: is missing but it is required")));

    }

    @Test
    public void testNotificationSentAfterSendingCreateCourseUnitMessage_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";
        String organisingOrganisationId2 = "SAV";
        String organisingOrganisationId3 = "JYU";

        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusYears(1));
        validity.setEnd(OffsetDateTime.now().plusYears(1));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisingOrganisationId);
        networkOrganisation.setValidityInNetwork(validity);
        networkOrganisation.setIsCoordinator(true);

        NetworkOrganisation networkOrganisation2 = new NetworkOrganisation();
        networkOrganisation2.setOrganisationTkCode(organisingOrganisationId2);
        networkOrganisation2.setValidityInNetwork(validity);
        networkOrganisation2.setIsCoordinator(false);

        NetworkOrganisation networkOrganisation3 = new NetworkOrganisation();
        networkOrganisation3.setOrganisationTkCode(organisingOrganisationId3);
        networkOrganisation3.setValidityInNetwork(validity);
        networkOrganisation3.setIsCoordinator(false);

        List<NetworkOrganisation> orgs = new ArrayList<>();
        orgs.add(networkOrganisation);
        orgs.add(networkOrganisation2);
        orgs.add(networkOrganisation3);

        Network network = DtoInitializerV8.getNetwork("CN-1", new LocalisedString("Verkosto", null, null), validity, orgs);
        networkService.create(modelMapper.map(network, NetworkEntity.class));

        // TUNI already persisted in setUp()
        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);

        Organisation organisation2 = DtoInitializerV8.getOrganisation(organisingOrganisationId2, organisingOrganisationId2);
        OrganisationEntity organisationEntity2 = modelMapper.map(organisation2, OrganisationEntity.class);
        organisationEntity2.setId(organisingOrganisationId2);
        organisationEntity2.setQueue(organisingOrganisationId2);
        organisationEntity2.setNotificationsEnabled(true);
        organisationEntity2.setSchemaVersion(messageSchemaVersion);
        organisationService.create(organisationEntity2);

        Organisation organisation3 = DtoInitializerV8.getOrganisation(organisingOrganisationId3, organisingOrganisationId3);
        OrganisationEntity organisationEntity3 = modelMapper.map(organisation3, OrganisationEntity.class);
        organisationEntity3.setId(organisingOrganisationId3);
        organisationEntity3.setQueue(organisingOrganisationId3);
        organisationEntity3.setNotificationsEnabled(false); // NOTE: this one doesn't want notifications :)
        organisationEntity3.setSchemaVersion(messageSchemaVersion);
        organisationService.create(organisationEntity3);

        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetwork = DtoInitializerV8.getCooperationNetwork(
                "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
                Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());

        // check if notification was received in organisation2...
        Message studyElementCreatedMessage = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId2);
        assertNotNull(studyElementCreatedMessage);

        CompositeIdentifiedEntityModifiedNotification notification = (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(studyElementCreatedMessage);

        assertNotNull(notification);
        assertNotNull(notification.getTimestamp());
        assertEquals(notification.getSendingOrganisationTkCode(), organisingOrganisationId);
        assertEquals("ID1", notification.getCreated().get(0).getReferenceIdentifier());
        assertEquals(organisingOrganisationId, notification.getCreated().get(0).getReferenceOrganizer());

        // ...but not sent to sender of the original create message...
        Message notificationInOrganisation1Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId);
        assertNull(notificationInOrganisation1Queue);

        // ...and not sent to organisation3 since "notificationsEnabled" was false
        Message notificationInOrganisation3Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId3);
        assertNull(notificationInOrganisation3Queue);
    }
    
    @Test
    public void testSendingCreateCourseUnitMessage_shouldSucceedCodeContainsSlash() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAI/RAI", courseUnitName,
                Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStatus(StudyStatus.ARCHIVED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode()).get();
        assertNotNull(createdCourseUnit);
        assertNotNull(createdCourseUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, createdCourseUnit.getType());

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(result.getStudyElementId(), result.getStudyElementId());
        assertEquals(result.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_shouldSucceed() throws JMSException, IOException, EntityNotFoundException {
        String organisingOrganisationId = "TUNI";
        persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitJson, MessageType.CREATE_COURSEUNIT_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                "78780", "TUNI").orElse(null);
        assertNotNull(createdUnit);
        assertEquals("78780", createdUnit.getStudyElementId());
        assertEquals("4SAVYONT", createdUnit.getStudyElementIdentifierCode());
        assertEquals("TUNI", createdUnit.getOrganizingOrganisationId());
        assertNotNull(createdUnit.getCreatedTime());
        assertNotNull(createdUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, createdUnit.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdUnit.getStatus());
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_containsSpecialCharactersAndXss_shouldSucceedAndTagsRemoved() throws JMSException, IOException, EntityNotFoundException {
        String organisingOrganisationId = "TUNI";
        persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitJsonWitSpecialCharacters,
            MessageType.CREATE_COURSEUNIT_REQUEST.name(), organisingOrganisationId);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            "78780", "TUNI").orElse(null);
        assertNotNull(createdUnit);
        assertEquals("78780", createdUnit.getStudyElementId());
        assertEquals("4SAVYONT", createdUnit.getStudyElementIdentifierCode());
        assertEquals("TUNI", createdUnit.getOrganizingOrganisationId());
        assertEquals("", createdUnit.getName().getValue("fi"));
        assertEquals("", createdUnit.getName().getValue("sv"));
        assertEquals("testFinal Thesis", createdUnit.getName().getValue("en"));
        assertNotNull(createdUnit.getCreatedTime());
        assertNotNull(createdUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, createdUnit.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdUnit.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_containsSpecialCharactersAndXss_shoudlSucceedAndTagsRemoved() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"en\": \"<script type='text/javascript'>xss here</script>\",\n" +
                "\t\t\t\t\"sv\": \"<strong>blaah</strong> jaksonnimi2 sv\",\n" +
                "\t\t\t\t\"fi\": \"<xc jaksonnimi2 fi\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("", result.getName().getValue("fi"));
        assertEquals("blaah jaksonnimi2 sv", result.getName().getValue("sv"));
        assertEquals("", result.getName().getValue("en"));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
                .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                        .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                                && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                                && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                                && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                                )));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_noCooperationNetworkNameOrStatusGiven_shouldFillCooperationNetworkFields() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organizingOrganisationId));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity.getId(),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("JUUJUU", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "           \"cooperationNetworks\": [\n" +
                "               {\n" +
                "                   \"id\": \"" + networkEntity.getId() +"\",\n" +
                "                   \"enrollable\": true\n" +
                "               }\n" +
                "           ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
                .allMatch(resultCn -> networkEntity.getId().equals(resultCn.getId())
                        && networkEntity.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                        && networkEntity.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                        && networkEntity.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                        ));
    }

    @Test
    public void testSendingUpdateCourseUnitMessageWithEmptyCooperationNetworks_shouldSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                        "\t\"courseUnit\": {\n" +
                        "\t\t\"studyElementId\": \"ID1\",\n" +
                        "\t\t\"name\": {\n" +
                        "\t\t\t\"values\": {\n" +
                        "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                        "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                        "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t},\n" +
                        "\t\t\"cooperationNetworks\": [\n" +
                        "\t\t\t]\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedContainsForwardSlash() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "I/D1", "RAI/RAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"I/D1\",\n" +
                "\t\t\"status\": \"ARCHIVED\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ARCHIVED, result.getStatus());
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
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv"));

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("ID2", code2, organisationId1, null,
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi 2 fi", "jaksonnimi 2 en", "jaksonnimi 2 sv"));

        CourseUnitEntity courseUnit3 = EntityInitializer.getCourseUnitEntity("ID3", code1, organisationId2, null,
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi 3 fi", "jaksonnimi 3 en", "jaksonnimi 3 sv"));
        
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
    public void testSendingCreateCourseUnitMessage_shouldFailOnJsonValidationError() throws JMSException {
        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitJsonMissingCooperationNetwork,
                MessageType.CREATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertEquals(1, resp.getErrors().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_missingStudyElementId_shouldFail() throws JMSException {
        String updateCourseUnitJson =
                "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertEquals(1, resp.getErrors().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailBrokenJson() throws JMSException {
        String updateCourseUnitJson =
                "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"IDASDASD\",,,,,,,\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedNoMatchingNetworksForRealisation() throws JMSException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity1 = persistNetworkEntity("CN-1", null, Collections.singletonList(organizingOrganisationId));
        NetworkEntity networkEntity2 = persistNetworkEntity("CN-3", null, Collections.singletonList(organizingOrganisationId));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity1.getId(), networkEntity1.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference reference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnit.getStudyElementId(), organizingOrganisationId);
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", organizingOrganisationId,
                Collections.singletonList(reference), Collections.singletonList(network));
        realisationRepository.create(realisationEntity);

        String updateCourseUnitJson =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi2 fi\",\n" +
                "                \"en\": \"jaksonnimi2 en\",\n" +
                "                \"sv\": \"jaksonnimi2 sv\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"cooperationNetworks\": [\n" +
                "            {\n" +
                "                \"id\": \"" + networkEntity2.getId() + "\",\n" +
                "                \"name\": {\n" +
                "                    \"values\": {\n" +
                "                        \"fi\": \"Verkosto 3\",\n" +
                "                        \"en\": \"Network 3\",\n" +
                "                        \"sv\": null\n" +
                "                    }\n" +
                "                },\n" +
                "                \"enrollable\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_teachingLanguageInAllCapsOrInvalid_shouldSucceed() throws JMSException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity1 = persistNetworkEntity("CN-1", null, Collections.singletonList(organizingOrganisationId));
        NetworkEntity networkEntity2 = persistNetworkEntity("CN-3", null, Collections.singletonList(organizingOrganisationId));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity1.getId(), networkEntity1.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference reference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organizingOrganisationId);
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", organizingOrganisationId,
            Collections.singletonList(reference), Collections.singletonList(network));
        realisationRepository.create(realisationEntity);

        String updateCourseUnitJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"teachingLanguage\": [\n" +
                "            \"FI\"\n" +
                "        ],\n" +
                "        \"languagesOfCompletion\": [\n" +
                "            \"EN\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> courseUnitEntities = StreamSupport.stream(courseUnitRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, courseUnitEntities.size());
        assertEquals(1, courseUnitEntities.get(0).getTeachingLanguage().size());
        assertEquals("fi", courseUnitEntities.get(0).getTeachingLanguage().get(0));
        assertEquals("en", courseUnitEntities.get(0).getLanguagesOfCompletion().get(0));

        updateCourseUnitJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"teachingLanguage\": [\n" +
                "            \"FICZXCX\"\n" +
                "        ],\n" +
                "        \"languagesOfCompletion\": [\n" +
                "            \"23423\"\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        courseUnitEntities = StreamSupport.stream(courseUnitRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, courseUnitEntities.size());
        assertEquals(0, courseUnitEntities.get(0).getTeachingLanguage().size());
        assertEquals(0, courseUnitEntities.get(0).getLanguagesOfCompletion().size());
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_shouldFailMessageHasTwoSameCourseUnits() throws JMSException, IOException, EntityNotFoundException {
        String organisingOrganisationId = "JUY";

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity(organisingOrganisationId, null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitJsonTwoSameCourseUnits, MessageType.CREATE_COURSEUNIT_REQUEST.name(), organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains("is found in message multiple times."));

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                "78780", "JUY").orElse(null);
        assertNull(createdUnit);
    }

    @Test
    public void testSendingCreateCourseUnitMessage_shouldFailRealisationMissingRequiredField() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        Realisation realisation = DtoInitializerV8.getRealisation("ID1", null, realisationName,
                Collections.singletonList(studyElementReference), null, null);

        courseUnit.setRealisations(Arrays.asList(realisation));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        // Expected errors:
        //$.courseUnits[0].realisations: should be valid to one and only one schema, but 0 are valid
        //$.courseUnits[0].realisations: array found, null expected
        //$.courseUnits[0].realisations[0].organisationReferences: null found, array expected
        //$.courseUnits[0].realisations: should be valid to one and only one schema, but 0 are valid
        //$.courseUnits[0].realisations: array found, null expected
        //$.courseUnits[0].realisations[0].cooperationNetworks: null found, array expected.
        assertEquals(6, resp.getErrors().size());
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithRealisations_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"),
            Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(networkEntity.getId(),
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true,
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        Realisation realisation = DtoInitializerV8.getRealisation("id1", "identifiercode", realisationName,
                Collections.singletonList(studyElementReference), Collections.singletonList(network), Arrays.asList(organisationReference));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        courseUnit.setRealisations(Collections.singletonList(realisation));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdCourseUnit);
        assertNotNull(createdCourseUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdCourseUnit.getStatus());
        assertEquals(organisation.getOrganisationTkCode(), createdCourseUnit.getOrganizingOrganisationId());

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(
            realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation);
        assertEquals(1, createdRealisation.getStudyElementReferences().size());
        assertEquals(courseUnit.getStudyElementId(), createdRealisation.getStudyElementReferences().get(0).getReferenceIdentifier());
        assertEquals(createdCourseUnit.getOrganizingOrganisationId(), createdRealisation.getStudyElementReferences().get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT, createdRealisation.getStudyElementReferences().get(0).getReferenceType());
        assertNotNull(createdRealisation.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdRealisation.getStatus());

        // Verify data denormalized
        assertEquals(1, createdCourseUnit.getRealisations().size());
        CourseUnitRealisationEntity courseUnitRealisation = createdCourseUnit.getRealisations().get(0);

        assertEquals(createdRealisation.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(createdRealisation.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(createdRealisation.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(createdRealisation.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(createdRealisation.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(createdRealisation.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(createdRealisation.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(createdRealisation.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(createdRealisation.getEnrollmentStartDateTime(), courseUnitRealisation.getEnrollmentStartDateTime());
        assertEquals(createdRealisation.getEnrollmentEndDateTime(), courseUnitRealisation.getEnrollmentEndDateTime());

    }

    @Test
    public void testSendingCreateCourseUnitMessageWithRealisationThatHasMissingCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializerV8.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
                Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForCourseUnit(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        Realisation realisation = DtoInitializerV8.getRealisation("id1", "identifiercode", realisationName,
                Collections.singletonList(studyElementReference), null, Arrays.asList(organisationReference));

        courseUnit.setRealisations(Arrays.asList(realisation));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        realisation = DtoInitializerV8.getRealisation("id1", "identifiercode", realisationName,
                Collections.singletonList(studyElementReference), Collections.emptyList(), Arrays.asList(organisationReference));

        courseUnit.setRealisations(Arrays.asList(realisation));

        req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithTwoAssessmentItemRealisations_shouldSuccess() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        fi.uta.ristiinopiskelu.datamodel.dto.v8.CooperationNetwork network = DtoInitializerV8.getCooperationNetwork(
                networkEntity.getId(), new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation organisation = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference organisationReference =
            DtoInitializerV8.getOrganisationReference(organisation, fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        CreateCourseUnitRequestDTO courseUnit = DtoInitializerV8.getCreateCourseUnitRequestDTO("ID1", "RAIRAI",
            courseUnitName, Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        AssessmentItem assessmentItem = DtoInitializerV8.getAssessmentItem("AI-1", new LocalisedString("arvioinninkohde 1", null, null));

        AssessmentItem assessmentItem2 = DtoInitializerV8.getAssessmentItem("AI-2", null);

        StudyElementReference studyElementReference = DtoInitializerV8.getStudyElementReferenceForAssessmentItem(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItem.getAssessmentItemId());

        StudyElementReference studyElementReference2 = DtoInitializerV8.getStudyElementReferenceForAssessmentItem(
                courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItem2.getAssessmentItemId());

        Realisation realisation = DtoInitializerV8.getRealisation("ID1", "IDENTIFIERCODE", new LocalisedString("nimi", null, null),
                Collections.singletonList(studyElementReference), Collections.singletonList(network), Arrays.asList(organisationReference));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        Realisation realisation2 = DtoInitializerV8.getRealisation("ID2", "IDENTIFIERCODE2", new LocalisedString("nimi2", null, null),
                Collections.singletonList(studyElementReference2), Collections.singletonList(network), Arrays.asList(organisationReference));
        realisation2.setStartDate(LocalDate.now().minusMonths(2));
        realisation2.setEndDate(LocalDate.now().plusMonths(2));
        realisation2.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(20));
        realisation2.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(20));

        assessmentItem.setRealisations(Collections.singletonList(realisation));
        assessmentItem2.setRealisations(Collections.singletonList(realisation2));
        CompletionOption completionOption = DtoInitializerV8.getCompletionOption("CO-1", "kuvaus", Collections.singletonList(assessmentItem),
                new LocalisedString("suoritustapa 1", null, null));

        CompletionOption completionOption2 = DtoInitializerV8.getCompletionOption("CO-2", "kuvaus", Collections.singletonList(assessmentItem2), null);

        courseUnit.setCompletionOptions(Arrays.asList(completionOption, completionOption2));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnit.getStudyElementId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdCourseUnit);
        assertNotNull(createdCourseUnit.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdCourseUnit.getStatus());
        assertEquals(organisation.getOrganisationTkCode(), createdCourseUnit.getOrganizingOrganisationId());
        assertEquals(completionOption.getName().getValue("fi"), createdCourseUnit.getCompletionOptions().stream()
                .filter(co -> co.getCompletionOptionId().equals(completionOption.getCompletionOptionId()))
                .findFirst().get().getName().getValue("fi"));
        assertNull(createdCourseUnit.getCompletionOptions().stream()
                .filter(co -> co.getCompletionOptionId().equals(completionOption2.getCompletionOptionId()))
                .findFirst().get().getName());
        assertEquals(assessmentItem.getName().getValue("fi"), createdCourseUnit.getCompletionOptions().stream()
                .filter(co -> co.getCompletionOptionId().equals(completionOption.getCompletionOptionId()))
                .findFirst().get()
                .getAssessmentItems()
                    .stream()
                    .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem.getAssessmentItemId()))
                    .findFirst().get().getName().getValue("fi"));
        assertNull(createdCourseUnit.getCompletionOptions().stream()
                .filter(co -> co.getCompletionOptionId().equals(completionOption2.getCompletionOptionId()))
                .findFirst().get()
                .getAssessmentItems()
                    .stream()
                    .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem2.getAssessmentItemId()))
                    .findFirst().get().getName());

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(realisation.getRealisationId(),
                organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation);
        assertEquals(1, createdRealisation.getStudyElementReferences().size());
        assertTrue(createdRealisation.getStudyElementReferences().stream().anyMatch(sr ->
                sr.getReferenceType().equals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.ASSESSMENT_ITEM)
                    && sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())
                    && sr.getReferenceAssessmentItemId().equals(assessmentItem.getAssessmentItemId())));
        assertNotNull(createdRealisation.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdRealisation.getStatus());

        RealisationEntity createdRealisation2 = realisationService.findByIdAndOrganizingOrganisationId(realisation2.getRealisationId(),
                organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation2);
        assertEquals(1, createdRealisation2.getStudyElementReferences().size());
        assertTrue(createdRealisation2.getStudyElementReferences().stream().anyMatch(sr ->
                sr.getReferenceType().equals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.ASSESSMENT_ITEM)
                    && sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())
                    && sr.getReferenceAssessmentItemId().equals(assessmentItem2.getAssessmentItemId())));

        assertNotNull(createdRealisation2.getCreatedTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, createdRealisation2.getStatus());

        // Verify data denormalized
        assertEquals(1, createdCourseUnit.getAssessmentItems().stream()
            .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem.getAssessmentItemId()))
            .findFirst().get().getRealisations().size());

        CourseUnitRealisationEntity courseUnitRealisation = createdCourseUnit.getAssessmentItems().stream()
            .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem.getAssessmentItemId()))
            .findFirst().get().getRealisations().stream()
            .filter(r -> r.getRealisationId().equals(realisation.getRealisationId())).findFirst().orElse(null);
        assertEquals(createdRealisation.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(createdRealisation.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(createdRealisation.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(createdRealisation.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(createdRealisation.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(createdRealisation.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(createdRealisation.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(createdRealisation.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(createdRealisation.getEnrollmentStartDateTime(), courseUnitRealisation.getEnrollmentStartDateTime());
        assertEquals(createdRealisation.getEnrollmentEndDateTime(), courseUnitRealisation.getEnrollmentEndDateTime());


        assertEquals(1, createdCourseUnit.getAssessmentItems().stream()
            .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem2.getAssessmentItemId()))
            .findFirst().get().getRealisations().size());

        CourseUnitRealisationEntity courseUnitRealisation2 = createdCourseUnit.getAssessmentItems().stream()
            .filter(ai -> ai.getAssessmentItemId().equals(assessmentItem2.getAssessmentItemId()))
            .findFirst().get().getRealisations().stream()
            .filter(r -> r.getRealisationId().equals(realisation2.getRealisationId())).findFirst().orElse(null);
        assertEquals(createdRealisation2.getRealisationId(), courseUnitRealisation2.getRealisationId());
        assertEquals(createdRealisation2.getOrganizingOrganisationId(), courseUnitRealisation2.getOrganizingOrganisationId());
        assertEquals(createdRealisation2.getRealisationIdentifierCode(), courseUnitRealisation2.getRealisationIdentifierCode());
        assertEquals(createdRealisation2.getName().getValue("fi"), courseUnitRealisation2.getName().getValue("fi"));
        assertEquals(createdRealisation2.getName().getValue("en"), courseUnitRealisation2.getName().getValue("en"));
        assertEquals(createdRealisation2.getName().getValue("sv"), courseUnitRealisation2.getName().getValue("sv"));
        assertEquals(createdRealisation2.getStartDate(), courseUnitRealisation2.getStartDate());
        assertEquals(createdRealisation2.getEndDate(), courseUnitRealisation2.getEndDate());
        assertEquals(createdRealisation2.getEnrollmentStartDateTime(), courseUnitRealisation2.getEnrollmentStartDateTime());
        assertEquals(createdRealisation2.getEnrollmentEndDateTime(), courseUnitRealisation2.getEnrollmentEndDateTime());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldSuccess() throws JMSException {

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
                Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));
        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", "1",
                Collections.singletonList(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("1", "1",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT)),
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
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        CourseUnitEntity courseUnit2 = EntityInitializer.getCourseUnitEntity("CU2", "TEST2", "1",
            Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("REAL1", "REAL1", "1",
            Collections.singletonList(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("CU1", "1",
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT)),
            Collections.emptyList());

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("REAL2", "REAL2", "1",
            Arrays.asList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("CU1", "1",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("CU2", "1",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT)),
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

        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference> references = realisationEntities.get(0).getStudyElementReferences();
        assertEquals(1, references.size());
        assertEquals(courseUnit2.getStudyElementId(), references.get(0).getReferenceIdentifier());
        assertEquals(courseUnit2.getOrganizingOrganisationId(), references.get(0).getReferenceOrganizer());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT, references.get(0).getReferenceType());
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldFail() throws JMSException {

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
                Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));
        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", "1",
                Collections.singletonList(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("1",  "1",
                    fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT)), Collections.emptyList());

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
                Collections.emptyList(),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference> references = new ArrayList<>();
        references.add(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("CU1",  "ORG1",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT));
        references.add(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("CU2",  "ORG2",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT));

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
    public void testSendingCreateCourseUnitMessage_shouldFailMissingMainlyResponsibleOrganisationRef() throws JMSException {
        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("JUY", null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitMissingMainlyResponsibleOrgRefJson,
                MessageType.CREATE_COURSEUNIT_REQUEST.name(), "JUY");

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains("is missing organizing organisation"));

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                "78780","JUY").orElse(null);
        assertNull(createdUnit);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndIgnoreType() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"type\": \"COURSE_UNIT\"," +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi2 fi\",\n" +
                "                \"en\": \"jaksonnimi2 en\",\n" +
                "                \"sv\": \"jaksonnimi2 sv\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
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
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference courseUnitRef =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference(courseUnit.getStudyElementId(),
                courseUnit.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference extraRef =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference("123",  "1",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

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
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndSuccessfullySaveHistory() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName = new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                        "    \"courseUnit\": {\n" +
                        "        \"studyElementId\": \"ID1\",\n" +
                        "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                        "        \"name\": {\n" +
                        "            \"values\": {\n" +
                        "                \"fi\": \"jaksonnimi2 fi\",\n" +
                        "                \"en\": \"jaksonnimi2 en\",\n" +
                        "                \"sv\": \"jaksonnimi2 sv\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity updatedCourseUnit = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), updatedCourseUnit.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), updatedCourseUnit.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", updatedCourseUnit.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", updatedCourseUnit.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", updatedCourseUnit.getName().getValue("sv"));
        assertEquals(2, updatedCourseUnit.getVersion());
        assertNotNull(updatedCourseUnit.getCreatedTime());
        assertNotNull(updatedCourseUnit.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, updatedCourseUnit.getType());

        List<CourseUnitEntity> historyEntities = HistoryHelper.queryHistoryIndex(
                elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        CourseUnitEntity historyEntity = historyEntities.get(0);
        assertEquals(courseUnit.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("en"), historyEntity.getName().getValue("en"));
        assertEquals(courseUnit.getName().getValue("sv"), historyEntity.getName().getValue("sv"));

        String courseUnitUpdateJson2 =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi3 fi\",\n" +
                "                \"en\": \"jaksonnimi3 en\",\n" +
                "                \"sv\": \"jaksonnimi3 sv\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson2, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity updatedCourseUnit2 = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), updatedCourseUnit2.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), updatedCourseUnit2.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi3 fi", updatedCourseUnit2.getName().getValue("fi"));
        assertEquals("jaksonnimi3 en", updatedCourseUnit2.getName().getValue("en"));
        assertEquals("jaksonnimi3 sv", updatedCourseUnit2.getName().getValue("sv"));
        assertEquals(3, updatedCourseUnit2.getVersion());
        assertNotNull(updatedCourseUnit2.getCreatedTime());
        assertNotNull(updatedCourseUnit2.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, updatedCourseUnit2.getType());

        historyEntities = HistoryHelper.queryHistoryIndex(
                elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(historyEntities);
        assertEquals(2, historyEntities.size());

        CourseUnitEntity newHistoryEntity = historyEntities.stream()
                .sorted(Comparator.comparing(CourseUnitEntity::getUpdateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .findFirst()
                .orElse(null);

        assertEquals(updatedCourseUnit.getName().getValue("fi"), newHistoryEntity.getName().getValue("fi"));
        assertEquals(updatedCourseUnit.getName().getValue("en"), newHistoryEntity.getName().getValue("en"));
        assertEquals(updatedCourseUnit.getName().getValue("sv"), newHistoryEntity.getName().getValue("sv"));
    }

    @Test
    public void testSendingDeleteCourseUnitMessageWithRealisations_shouldSuccessAndCreateHistory() throws JMSException {
        
        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("1", null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("1", "TEST1", "1",
                Collections.emptyList(), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", "test", "test"));

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("2", "TEST2", courseUnit.getOrganizingOrganisationId(),
                Collections.singletonList(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference(courseUnit.getStudyElementId(),
                    courseUnit.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT)),
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
                        && sr.getReferenceType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailHasExtraField() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"type\": \"COURSE_UNIT\"," +
                "        \"thisdoesnotexist\": \"somerandomcontent\"" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_shouldFailJsonHasUnknownFields() throws JMSException, IOException, EntityNotFoundException {
        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitWithUnknownFieldsJson, MessageType.CREATE_COURSEUNIT_REQUEST.name(), "JUY");

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.FAILED);

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                "78780", "JUY").orElse(null);
        assertNull(createdUnit);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailStatusNull() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
                "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"status\": null\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksEmpty() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"cooperationNetworks\": []\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(0, result.getCooperationNetworks().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksNull() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"cooperationNetworks\": null\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(null, result.getCooperationNetworks());
    }


    @Test
    public void testSendingUpdateCourseUnitMessage_containsRealisations_shouldNotUpdateRealisationInfoAndSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
            "    \"courseUnit\": {\n" +
            "        \"studyElementId\": \"ID1\",\n" +
            "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
            "        \"name\": {\n" +
            "            \"values\": {\n" +
            "                \"fi\": \"jaksonnimi 2 fi\",\n" +
            "                \"en\": \"jaksonnimi 2 en\",\n" +
            "                \"sv\": \"jaksonnimi 2 sv\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"completionOptions\": [\n" +
            "            {\n" +
            "                \"assessmentItems\": [\n" +
            "                    {\n" +
            "                        \"primaryCourseUnitId\": null,\n" +
            "                        \"name\": {\n" +
            "                            \"values\": {\n" +
            "                                \"fi\": \"arvioinninkohde 1\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"realisations\": [\n" +
            "                            {\n" +
            "                                \"realisationId\": \"ID1\",\n" +
            "                                \"realisationIdentifierCode\": \"IDENTIFIERCODE\",\n" +
            "                                \"selections\": null,\n" +
            "                                \"questionSets\": null,\n" +
            "                                \"enrollmentStartDateTime\": \"2019-12-24T15:55:52.134+02:00\",\n" +
            "                                \"enrollmentEndDateTime\": \"2020-01-23T15:55:52.134+02:00\",\n" +
            "                                \"startDate\": \"2019-12-08\",\n" +
            "                                \"endDate\": \"2020-02-08\",\n" +
            "                                \"personReferences\": null,\n" +
            "                                \"minSeats\": null,\n" +
            "                                \"maxSeats\": null,\n" +
            "                                \"location\": null,\n" +
            "                                \"organisationSpecificDescriptions\": null,\n" +
            "                                \"studyElementReferences\": [\n" +
            "                                    {\n" +
            "                                        \"referenceIdentifier\": \"ID1\",\n" +
            "                                        \"referenceOrganizer\": \"TUNI\",\n" +
            "                                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
            "                                        \"referenceAssessmentItemId\": \"AI-1\"\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"name\": {\n" +
            "                                    \"values\": {\n" +
            "                                        \"fi\": \"nimi\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"organisationReferences\": [\n" +
            "                                    {\n" +
            "                                        \"percent\": 100,\n" +
            "                                        \"organisationRole\": 1,\n" +
            "                                        \"target\": {\n" +
            "                                            \"organisationIdentifier\": \"TUNI\",\n" +
            "                                            \"organisationTkCode\": \"TUNI\",\n" +
            "                                            \"organisationName\": null,\n" +
            "                                            \"unitName\": null,\n" +
            "                                            \"nameDescription\": null,\n" +
            "                                            \"postalAddress\": null,\n" +
            "                                            \"streetAddress\": null,\n" +
            "                                            \"municipalityCode\": null,\n" +
            "                                            \"phone\": null,\n" +
            "                                            \"url\": null\n" +
            "                                        },\n" +
            "                                        \"description\": null\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"groupSelections\": null,\n" +
            "                                \"cooperationNetworks\": [\n" +
            "                                    {\n" +
            "                                        \"validityStartDate\": \"2019-01-08\",\n" +
            "                                        \"validityEndDate\": \"2021-01-08\",\n" +
            "                                        \"enrollable\": true,\n" +
            "                                        \"id\": \"CN-1\",\n" +
            "                                        \"name\": {\n" +
            "                                            \"values\": {\n" +
            "                                                \"fi\": \"Verkosto 1\",\n" +
            "                                                \"sv\": \"Verkosto sv\",\n" +
            "                                                \"en\": \"Verkosto en\"\n" +
            "                                            }\n" +
            "                                        }\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"createdTime\": null,\n" +
            "                                \"updateTime\": null,\n" +
            "                                \"groupQuotas\": null,\n" +
            "                                \"status\": null,\n" +
            "                                \"creditsMin\": null,\n" +
            "                                \"creditsMax\": null,\n" +
            "                                \"teachingLanguage\": null,\n" +
            "                                \"minEduGuidanceArea\": null\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"realisationId\": \"ID2\",\n" +
            "                                \"realisationIdentifierCode\": \"IDENTIFIERCODE2\",\n" +
            "                                \"selections\": null,\n" +
            "                                \"questionSets\": null,\n" +
            "                                \"enrollmentStartDateTime\": \"2019-12-19T15:55:52.134+02:00\",\n" +
            "                                \"enrollmentEndDateTime\": \"2020-01-28T15:55:52.134+02:00\",\n" +
            "                                \"startDate\": \"2019-11-08\",\n" +
            "                                \"endDate\": \"2020-03-08\",\n" +
            "                                \"personReferences\": null,\n" +
            "                                \"minSeats\": null,\n" +
            "                                \"maxSeats\": null,\n" +
            "                                \"location\": null,\n" +
            "                                \"organisationSpecificDescriptions\": null,\n" +
            "                                \"studyElementReferences\": [\n" +
            "                                    {\n" +
            "                                        \"referenceIdentifier\": \"ID1\",\n" +
            "                                        \"referenceOrganizer\": \"TUNI\",\n" +
            "                                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
            "                                        \"referenceAssessmentItemId\": \"AI-2\"\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"name\": {\n" +
            "                                    \"values\": {\n" +
            "                                        \"fi\": \"nimi2\"\n" +
            "                                    }\n" +
            "                                },\n" +
            "                                \"organisationReferences\": [\n" +
            "                                    {\n" +
            "                                        \"percent\": 100,\n" +
            "                                        \"organisationRole\": 1,\n" +
            "                                        \"target\": {\n" +
            "                                            \"organisationIdentifier\": \"TUNI\",\n" +
            "                                            \"organisationTkCode\": \"TUNI\",\n" +
            "                                            \"organisationName\": null,\n" +
            "                                            \"unitName\": null,\n" +
            "                                            \"nameDescription\": null,\n" +
            "                                            \"postalAddress\": null,\n" +
            "                                            \"streetAddress\": null,\n" +
            "                                            \"municipalityCode\": null,\n" +
            "                                            \"phone\": null,\n" +
            "                                            \"url\": null\n" +
            "                                        },\n" +
            "                                        \"description\": null\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"groupSelections\": null,\n" +
            "                                \"cooperationNetworks\": [\n" +
            "                                    {\n" +
            "                                        \"validityStartDate\": \"2019-01-08\",\n" +
            "                                        \"validityEndDate\": \"2021-01-08\",\n" +
            "                                        \"enrollable\": true,\n" +
            "                                        \"id\": \"CN-1\",\n" +
            "                                        \"name\": {\n" +
            "                                            \"values\": {\n" +
            "                                                \"fi\": \"Verkosto 1\",\n" +
            "                                                \"sv\": \"Verkosto sv\",\n" +
            "                                                \"en\": \"Verkosto en\"\n" +
            "                                            }\n" +
            "                                        }\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"createdTime\": null,\n" +
            "                                \"updateTime\": null,\n" +
            "                                \"groupQuotas\": null,\n" +
            "                                \"status\": null,\n" +
            "                                \"creditsMin\": null,\n" +
            "                                \"creditsMax\": null,\n" +
            "                                \"teachingLanguage\": null,\n" +
            "                                \"minEduGuidanceArea\": null\n" +
            "                            }\n" +
            "                        ],\n" +
            "                        \"type\": null,\n" +
            "                        \"assessmentItemId\": \"AI-1\",\n" +
            "                        \"creditsMin\": null,\n" +
            "                        \"creditsMax\": null\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": {\n" +
            "                    \"values\": {\n" +
            "                        \"fi\": \"suoritustapa 1\"\n" +
            "                    }\n" +
            "                },\n" +
            "                \"completionOptionId\": \"CO-1\",\n" +
            "                \"description\": \"kuvaus\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"assessmentItems\": [\n" +
            "                    {\n" +
            "                        \"primaryCourseUnitId\": null,\n" +
            "                        \"name\": null,\n" +
            "                        \"realisations\": null,\n" +
            "                        \"type\": null,\n" +
            "                        \"assessmentItemId\": \"AI-2\",\n" +
            "                        \"creditsMin\": null,\n" +
            "                        \"creditsMax\": null\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": null,\n" +
            "                \"completionOptionId\": \"CO-2\",\n" +
            "                \"description\": \"kuvaus\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"realisations\": [\n" +
            "            {\n" +
            "                \"realisationId\": \"ID3\",\n" +
            "                \"realisationIdentifierCode\": \"IDENTIFIERCODE3\",\n" +
            "                \"selections\": null,\n" +
            "                \"questionSets\": null,\n" +
            "                \"enrollmentStartDateTime\": \"2019-12-19T15:55:52.134+02:00\",\n" +
            "                \"enrollmentEndDateTime\": \"2020-01-28T15:55:52.134+02:00\",\n" +
            "                \"startDate\": \"2019-11-08\",\n" +
            "                \"endDate\": \"2020-03-08\",\n" +
            "                \"personReferences\": null,\n" +
            "                \"minSeats\": null,\n" +
            "                \"maxSeats\": null,\n" +
            "                \"location\": null,\n" +
            "                \"organisationSpecificDescriptions\": null,\n" +
            "                \"studyElementReferences\": [\n" +
            "                    {\n" +
            "                        \"referenceIdentifier\": \"ID1\",\n" +
            "                        \"referenceOrganizer\": \"TUNI\",\n" +
            "                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
            "                        \"referenceAssessmentItemId\": \"AI-2\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": {\n" +
            "                    \"values\": {\n" +
            "                        \"fi\": \"nimi3\"\n" +
            "                    }\n" +
            "                },\n" +
            "                \"organisationReferences\": [\n" +
            "                    {\n" +
            "                        \"percent\": 100,\n" +
            "                        \"organisationRole\": 1,\n" +
            "                        \"target\": {\n" +
            "                            \"organisationIdentifier\": \"TUNI\",\n" +
            "                            \"organisationTkCode\": \"TUNI\",\n" +
            "                            \"organisationName\": null,\n" +
            "                            \"unitName\": null,\n" +
            "                            \"nameDescription\": null,\n" +
            "                            \"postalAddress\": null,\n" +
            "                            \"streetAddress\": null,\n" +
            "                            \"municipalityCode\": null,\n" +
            "                            \"phone\": null,\n" +
            "                            \"url\": null\n" +
            "                        },\n" +
            "                        \"description\": null\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"groupSelections\": null,\n" +
            "                \"cooperationNetworks\": [\n" +
            "                    {\n" +
            "                        \"validityStartDate\": \"2019-01-08\",\n" +
            "                        \"validityEndDate\": \"2021-01-08\",\n" +
            "                        \"enrollable\": true,\n" +
            "                        \"id\": \"CN-1\",\n" +
            "                        \"name\": {\n" +
            "                            \"values\": {\n" +
            "                                \"fi\": \"Verkosto 1\",\n" +
            "                                \"sv\": \"Verkosto sv\",\n" +
            "                                \"en\": \"Verkosto en\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"createdTime\": null,\n" +
            "                \"updateTime\": null,\n" +
            "                \"groupQuotas\": null,\n" +
            "                \"status\": null,\n" +
            "                \"creditsMin\": null,\n" +
            "                \"creditsMax\": null,\n" +
            "                \"teachingLanguage\": null,\n" +
            "                \"minEduGuidanceArea\": null\n" +
            "            }\n" +
            "        ],\n" +
            "        \"sendingTime\": \"2020-01-08T15:55:52.134+02:00\",\n" +
            "        \"creditsMin\": 2.5,\n" +
            "        \"creditsMax\": 5,\n" +
            "        \"organisationReferences\": [\n" +
            "            {\n" +
            "                \"percent\": 100,\n" +
            "                \"organisationRole\": 1,\n" +
            "                \"target\": {\n" +
            "                    \"organisationIdentifier\": \"TUNI\",\n" +
            "                    \"organisationTkCode\": \"TUNI\",\n" +
            "                    \"organisationName\": null,\n" +
            "                    \"unitName\": null,\n" +
            "                    \"nameDescription\": null,\n" +
            "                    \"postalAddress\": null,\n" +
            "                    \"streetAddress\": null,\n" +
            "                    \"municipalityCode\": null,\n" +
            "                    \"phone\": null,\n" +
            "                    \"url\": null\n" +
            "                },\n" +
            "                \"description\": null\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi 2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi 2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi 2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                    && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                    && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                    && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                )));
        assertNull(result.getRealisations());
        assertNull(result.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations());
    }


    @Test
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldFail() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(removedAssessmentItemEntity));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);

        courseUnitRepository.create(courseUnit);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference removedReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), removedAssessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference), Collections.singletonList(network));

        realisationRepository.create(realisation);

        RealisationEntity realisationWithRefToRemovedAssessmentItem = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(removedReference), Collections.singletonList(network));

        realisationRepository.create(realisationWithRefToRemovedAssessmentItem);

        String updateJson =
            "{\n" +
            "    \"courseUnit\": {\n" +
            "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
            "        \"completionOptions\": [\n" +
            "            {\n" +
            "                \"completionOptionId\": \"" + completionOptionEntity.getCompletionOptionId() + "\",\n" +
            "                \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\",\n" +
            "                \"assessmentItems\": [\n" +
            "                    {\n" +
            "                        \"assessmentItemId\": \"" + assessmentItemEntity.getAssessmentItemId() + "\",\n" +
            "                        \"primaryCourseUnitId\": null,\n" +
            "                        \"name\": {\n" +
            "                            \"values\": {\n" +
            "                                \"fi\": \"arvioinninkohde 1\"\n" +
            "                            }\n" +
            "                         }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            {\n" +
            "                \"completionOptionId\": \"" + completionOptionEntity2.getCompletionOptionId() + "\",\n" +
            "                \"description\": \"completion option in which assessment item is removed that has realisation reference \",\n" +
            "                \"assessmentItems\": [\n" +
            "                    {\n" +
            "                        \"assessmentItemId\": \"NEWASSESSMENTITEM\",\n" +
            "                        \"primaryCourseUnitId\": null,\n" +
            "                        \"name\": {\n" +
            "                            \"values\": {\n" +
            "                                \"fi\": \"uusi arvioinninkohde\"\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains(removedAssessmentItemEntity.getAssessmentItemId()));
        assertTrue(resp.getMessage().contains(realisationWithRefToRemovedAssessmentItem.getRealisationId()));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldReAddDenormalizedData() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-3", "ID3");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Arrays.asList(assessmentItemEntity2, removedAssessmentItemEntity));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString originalName =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.ACTIVE);

        courseUnitRepository.create(courseUnit);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity2.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Arrays.asList(studyElementReference), Collections.singletonList(network));
        realisation.setName(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Toteutus 1", "Realisation1", "Real1"));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        realisationRepository.create(realisation);

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference2), Collections.singletonList(network));
        realisation2.setName(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Toteutus 2", "Realisation2", "Real2"));
        realisation2.setStartDate(LocalDate.now().minusMonths(2));
        realisation2.setEndDate(LocalDate.now().plusMonths(2));
        realisation2.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(20));
        realisation2.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(20));

        realisationRepository.create(realisation2);

        String updateJson =
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"completionOptions\": [\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"" + assessmentItemEntity.getAssessmentItemId() + "\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"arvioinninkohde 1\"\n" +
                "                            }\n" +
                "                         }\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity2.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"completion option in which one assessment item is removed \",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"" + assessmentItemEntity2.getAssessmentItemId() + "\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"uusi arvioinninkohde\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ]" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).orElse(null);

        // Verify data denormalized
        List<CourseUnitRealisationEntity> courseUnitRealisations = createdCourseUnit.getCompletionOptions().stream()
            .filter(co -> co.getCompletionOptionId().equals(completionOptionEntity.getCompletionOptionId())).findFirst().get()
            .getAssessmentItems().stream().filter(ai -> ai.getAssessmentItemId().equals(assessmentItemEntity.getAssessmentItemId())).findFirst().get()
            .getRealisations();

        assertEquals(1, courseUnitRealisations.size());
        CourseUnitRealisationEntity courseUnitRealisation = courseUnitRealisations.get(0);

        assertEquals(realisation.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(realisation.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(realisation.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(realisation.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(realisation.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(realisation.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(realisation.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(realisation.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(DateUtils.getFormatted(realisation.getEnrollmentStartDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(realisation.getEnrollmentEndDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentEndDateTime()));

        courseUnitRealisations = createdCourseUnit.getCompletionOptions().stream()
            .filter(co -> co.getCompletionOptionId().equals(completionOptionEntity2.getCompletionOptionId())).findFirst().get()
            .getAssessmentItems().stream().filter(ai -> ai.getAssessmentItemId().equals(assessmentItemEntity2.getAssessmentItemId())).findFirst().get()
            .getRealisations();

        assertEquals(1, courseUnitRealisations.size());
        courseUnitRealisation = courseUnitRealisations.get(0);

        assertEquals(realisation2.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(realisation2.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(realisation2.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(realisation2.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(realisation2.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(realisation2.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(realisation2.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(realisation2.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(DateUtils.getFormatted(realisation2.getEnrollmentStartDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(realisation2.getEnrollmentEndDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentEndDateTime()));
    }

    // TODO: move to a separate service layer integration test someday?
    @Test
    public void testCreatingDuplicateCourseUnits_shouldFail() {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation org = DtoInitializer.getOrganisation("TESTORG", "TESTORG");
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org,
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("TESTORG", "blaa",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("test", null, null), 8);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("TESTID", "TESTORG", null, null);
        courseUnitEntity.setOrganisationReferences(Collections.singletonList(organisationRef));

        courseUnitService.create(courseUnitEntity);
        courseUnitEntity.setId(null);
        assertThrows(CreateFailedException.class, () -> courseUnitService.create(courseUnitEntity));
    }

    private NetworkEntity persistNetworkEntity(String id, fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString name,
                                               List<String> organisationIds) {
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

    private final String createCourseUnitJson =
            "{\n" +
            "    \"courseUnits\": [\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"validityStartDate\": \"2019-06-06\",\n" +
            "            \"personReferences\": [\n" +
            "               {\n" +
            "                   \"personRole\": {\n" +
            "                       \"key\": \"asd\",\n" +
            "                       \"codeSetKey\": \"dasd\"\n" +
            "                   },\n" +
            "                   \"person\": {\n" +
            "                       \"homeEppn\": \"1234\",\n" +
            "                       \"hostEppn\": null,\n" +
            "                       \"firstNames\": \"Jaska\",\n" +
            "                       \"givenName\": \"J\",\n" +
            "                       \"surName\": \"Jokunen\"\n" +
            "                   },\n" +
            "                   \"definition\": \"\"\n" +
            "               }\n" +
            "            ]," +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000+03:00\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"EN\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"Opinnäytetyö\",\n" +
            "                    \"en\": \"Final Thesis\",\n" +
            "                    \"sv\": null\n" +
            "                }\n" +
            "            },\n" +
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "                {\n" +
            "                    \"id\": \"CN-1\",\n" +
            "                    \"name\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"Verkosto 1\",\n" +
            "                            \"en\": \"Network 1\",\n" +
            "                            \"sv\": null\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"enrollable\": true\n" +
            "                }\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"TUNI\",\n" +
            "                        \"organisationTkCode\": \"TUNI\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"keywords\": [\n" +
            "                {\n" +
            "                    \"key\": \"testkey\",\n" +
            "                    \"value\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"value fi\",\n" +
            "                            \"en\": \"value en\",\n" +
            "                            \"sv\": \"value sv\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"keySet\": \"testkeyset\"\n" +
            "                }\n" +
            "            ]" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String createCourseUnitJsonWitSpecialCharacters =
        "{\n" +
            "    \"courseUnits\": [\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"validityStartDate\": \"2019-06-06\",\n" +
            "            \"personReferences\": [\n" +
            "               {\n" +
            "                   \"personRole\": {\n" +
            "                       \"key\": \"asd\",\n" +
            "                       \"codeSetKey\": \"dasd\"\n" +
            "                   },\n" +
            "                   \"person\": {\n" +
            "                       \"homeEppn\": \"1234\",\n" +
            "                       \"hostEppn\": null,\n" +
            "                       \"firstNames\": \"Jaska\",\n" +
            "                       \"givenName\": \"J\",\n" +
            "                       \"surName\": \"Jokunen\"\n" +
            "                   },\n" +
            "                   \"definition\": \"\"\n" +
            "               }\n" +
            "            ]," +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000+03:00\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"en\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"<xc\",\n" +
            "                    \"en\": \"<b>test</b>Final Thesis\",\n" +
            "                    \"sv\": \"<script type='text/javascript'>xss test</script>\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "                {\n" +
            "                    \"id\": \"CN-1\",\n" +
            "                    \"name\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"Verkosto 1 < dsac > <> #!#¤¤&%&/(@\",\n" +
            "                            \"en\": \"Network 1\",\n" +
            "                            \"sv\": null\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"enrollable\": true\n" +
            "                }\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"TUNI\",\n" +
            "                        \"organisationTkCode\": \"TUNI\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"keywords\": [\n" +
            "                {\n" +
            "                    \"key\": \"testkey\",\n" +
            "                    \"value\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"value fi\",\n" +
            "                            \"en\": \"value en\",\n" +
            "                            \"sv\": \"value sv\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"keySet\": \"testkeyset\"\n" +
            "                }\n" +
            "            ]" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String createCourseUnitJsonMissingCooperationNetwork =
            "{\n" +
            "    \"courseUnits\": [\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000Z\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"en\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"Opinnäytetyö\",\n" +
            "                    \"en\": \"Final Thesis\",\n" +
            "                    \"sv\": null\n" +
            "                }\n" +
            "            },\n" +
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"JUY\",\n" +
            "                        \"organisationTkCode\": \"JUY\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String createCourseUnitJsonTwoSameCourseUnits =
            "{\n" +
            "    \"courseUnits\": [\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000Z\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"en\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"Opinnäytetyö\",\n" +
            "                    \"en\": \"Final Thesis\",\n" +
            "                    \"sv\": null\n" +
            "                }\n" +
            "            },\n" +
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "                {\n" +
            "                    \"id\": \"CN-1\",\n" +
            "                    \"name\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"Verkosto 1\",\n" +
            "                            \"en\": \"Network 1\",\n" +
            "                            \"sv\": null\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"enrollable\": true\n" +
            "                }\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"JUY\",\n" +
            "                        \"organisationTkCode\": \"JUY\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000Z\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"en\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"Opinnäytetyö\",\n" +
            "                    \"en\": \"Final Thesis\",\n" +
            "                    \"sv\": null\n" +
            "                }\n" +
            "            },\n" +
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "                {\n" +
            "                    \"id\": \"CN-1\",\n" +
            "                    \"name\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"Verkosto 1\",\n" +
            "                            \"en\": \"Network 1\",\n" +
            "                            \"sv\": null\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"enrollable\": true\n" +
            "                }\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"JUY\",\n" +
            "                        \"organisationTkCode\": \"JUY\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private final String createCourseUnitMissingMainlyResponsibleOrgRefJson =
            "{\n" +
            "	\"courseUnits\": [\n" +
            "		{\n" +
            "           \"status\": \"ACTIVE\",\n " +
            "			\"studyElementId\": \"78780\",\n" +
            "			\"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "			\"type\": \"COURSE_UNIT\",\n" +
            "			\"abbreviation\": \"Opinetyö\",\n" +
            "			\"validityStartDate\": \"2019-06-06\",\n" +
            "           \"personReferences\": [\n" +
            "               {\n" +
            "                   \"personRole\": {\n" +
            "                     \"key\": \"asd\",\n" +
            "                     \"codeSetKey\": \"dasd\"\n" +
            "                   },\n" +
            "                   \"person\": {\n" +
            "                      \"homeEppn\": \"1234\",\n" +
            "                      \"hostEppn\": null,\n" +
            "                      \"firstNames\": \"Jaska\",\n" +
            "                      \"givenName\": \"J\",\n" +
            "                      \"surName\": \"Jokunen\"\n" +
            "                   },\n" +
            "                   \"definition\": \"\"\n" +
            "               }\n" +
            "           ]," +
            "			\"sendingTime\": \"2019-06-06T21:00:00.000+03:00\",\n" +
            "			\"teachingLanguage\": [\n" +
            "				\"en\"\n" +
            "			],\n" +
            "           \"assessmentScale\": 1,\n" +
            "			\"name\": {\n" +
            "				\"values\": {\n" +
            "					\"fi\": \"Opinnäytetyö\",\n" +
            "					\"en\": \"Final Thesis\",\n" +
            "					\"sv\": null\n" +
            "				}\n" +
            "			},\n" +
            "			\"learningMaterials\": null,\n" +
            "			\"groupSize\": 122,\n" +
            "			\"completionOptions\": [\n" +
            "				{\n" +
            "					\"completionOptionId\": \"yfIE\",\n" +
            "					\"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "				}\n" +
            "			],\n" +
            "			\"creditsMin\": 15,\n" +
            "			\"creditsMax\": 35,\n" +
            "			\"cooperationNetworks\": [\n" +
            "				{\n" +
            "					\"id\": \"CN-1\",\n" +
            "					\"name\": {\n" +
            "						\"values\": {\n" +
            "							\"fi\": \"Verkosto 1\",\n" +
            "							\"en\": \"Network 1\",\n" +
            "							\"sv\": null\n" +
            "						}\n" +
            "					},\n" +
            "					\"enrollable\": true\n" +
            "				}\n" +
            "			],\n" +
            "			\"organisationReferences\": [\n" +
            "				{\n" +
            "					\"organisationRole\": 2,\n" +
            "					\"target\": {\n" +
            "						\"organisationIdentifier\": \"JUY\",\n" +
            "						\"organisationTkCode\": \"JUY\"\n" +
            "					}\n" +
            "				}\n" +
            "			]\n" +
            "		}\n" +
            "	]\n" +
            "}";

    private final String createCourseUnitWithUnknownFieldsJson =
            "{\n" +
            "    \"courseUnits\": [\n" +
            "        {\n" +
            "            \"studyElementId\": \"78780\",\n" +
            "            \"studyElementIdentifierCode\": \"4SAVYONT\",\n" +
            "            \"type\": \"COURSE_UNIT\",\n" +
            "            \"abbreviation\": \"Opinetyö\",\n" +
            "            \"validityStartDate\": \"2019-06-06\",\n" +
            "            \"personReferences\": [\n" +
            "               {\n" +
            "                   \"personRole\": {\n" +
            "                       \"key\": \"asd\",\n" +
            "                       \"codeSetKey\": \"dasd\"\n" +
            "                   },\n" +
            "                   \"person\": {\n" +
            "                       \"homeEppn\": \"1234\",\n" +
            "                       \"hostEppn\": null,\n" +
            "                       \"firstNames\": \"Jaska\",\n" +
            "                       \"givenName\": \"J\",\n" +
            "                       \"surName\": \"Jokunen\"\n" +
            "                   },\n" +
            "                   \"definition\": \"\"\n" +
            "               }\n" +
            "            ]," +
            "            \"sendingTime\": \"2019-06-06T21:00:00.000+03:00\",\n" +
            "            \"teachingLanguage\": [\n" +
            "                \"en\"\n" +
            "            ],\n" +
            "            \"assessmentScale\": 1,\n" +
            "            \"name\": {\n" +
            "                \"values\": {\n" +
            "                    \"fi\": \"Opinnäytetyö\",\n" +
            "                    \"en\": \"Final Thesis\",\n" +
            "                    \"sv\": null\n" +
            "                }\n" +
            "            },\n" +
            "            \"studyElementStatus\": \"RANDOM_VALUE\"," +       // unknown field
            "            \"anotherRandomField\": \"RANDOM_VALUE2\"," +      // unknown field
            "            \"learningMaterials\": null,\n" +
            "            \"groupSize\": 122,\n" +
            "            \"completionOptions\": [\n" +
            "                {\n" +
            "                    \"completionOptionId\": \"yfIE\",\n" +
            "                    \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"creditsMin\": 15,\n" +
            "            \"creditsMax\": 35,\n" +
            "            \"cooperationNetworks\": [\n" +
            "                {\n" +
            "                    \"id\": \"CN-1\",\n" +
            "                    \"name\": {\n" +
            "                        \"values\": {\n" +
            "                            \"fi\": \"Verkosto 1\",\n" +
            "                            \"en\": \"Network 1\",\n" +
            "                            \"sv\": null\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"enrollable\": true\n" +
            "                }\n" +
            "            ],\n" +
            "            \"organisationReferences\": [\n" +
            "                {\n" +
            "					 \"organisationRole\": 1,\n" +
            "                    \"target\": {\n" +
            "                        \"organisationIdentifier\": \"JUY\",\n" +
            "                        \"organisationTkCode\": \"JUY\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

}


