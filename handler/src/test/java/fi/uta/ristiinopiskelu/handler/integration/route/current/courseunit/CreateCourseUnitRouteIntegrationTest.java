package fi.uta.ristiinopiskelu.handler.integration.route.current.courseunit;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CompletionOptionWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializerV8;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.UpdateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
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
import org.springframework.data.domain.Pageable;
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
public class CreateCourseUnitRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateCourseUnitRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private OrganisationService organisationService;

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
                organisation.setSchemaVersion(this.messageSchemaVersion);
                organisationService.create(organisation);
            }
        }
    }

    @Test
    public void testSendingCreateCourseUnitMessage_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, createdCourseUnit.getType());
        assertEquals(courseUnit.getStudyElementId(), createdCourseUnit.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), createdCourseUnit.getStudyElementIdentifierCode());
        assertEquals(courseUnit.getStudyElementPermanentId(), createdCourseUnit.getStudyElementPermanentId());
        assertEquals(1, createdCourseUnit.getCooperationNetworks().size());

        // Verify cooperation network data was gathered from network index
        CooperationNetworkEntity courseUnitNetwork = createdCourseUnit.getCooperationNetworks().get(0);
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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.CANCELLED, result.getStatus());
        assertNotEquals(result.getId(), courseUnit.getStudyElementId());
        assertNotEquals(result.getId(), courseUnit.getStudyElementIdentifierCode());
        assertNotEquals(result.getId(), courseUnit.getStudyElementPermanentId());
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertEquals(courseUnit.getStudyElementPermanentId(), result.getStudyElementPermanentId());
    }

    @Test
    public void testSendingCreateCourseUnitMessage_withBothCompletionOptionDescriptions_shouldFail() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO1", "kuvaus", List.of(), new LocalisedString("Nimi"));
        completionOption.setDescriptionLocalised(new LocalisedString("Tuplakuvaus lokalisoituna", "Double description", "Dubbel deskriptionen"));

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);
        courseUnit.setCompletionOptions(List.of(completionOption));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.FAILED, resp.getStatus());
    }

    @Test
    public void testSendingCreateCourseUnitMessage_withCompletionOptionLocalisedDescriptionOnly_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO1", null, List.of(), new LocalisedString("Nimi"));
        completionOption.setDescriptionLocalised(new LocalisedString("Kuvaus lokalisoituna", "Description localised", "Deskriptionen"));

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);
        courseUnit.setCompletionOptions(List.of(completionOption));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false)
            .toList();
        assertNotNull(savedCourseUnits);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(completionOption.getDescriptionLocalised().getValue(Language.FI), result.getCompletionOptions().get(0).getDescription().getValue(Language.FI));
        assertEquals(completionOption.getDescriptionLocalised().getValue(Language.SV), result.getCompletionOptions().get(0).getDescription().getValue(Language.SV));
        assertEquals(completionOption.getDescriptionLocalised().getValue(Language.EN), result.getCompletionOptions().get(0).getDescription().getValue(Language.EN));
    }

    @Test
    public void testSendingCreateCourseUnitMessage_withCompletionOptionSimpleDescriptionOnly_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO1", "Vanha kuvaus ei lokalisoituna", List.of(), new LocalisedString("Nimi"));

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);
        courseUnit.setCompletionOptions(List.of(completionOption));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false)
            .toList();
        assertNotNull(savedCourseUnits);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(completionOption.getDescription(), result.getCompletionOptions().get(0).getDescription().getValue(Language.FI));
        assertEquals("", result.getCompletionOptions().get(0).getDescription().getValue(Language.SV));
        assertEquals("", result.getCompletionOptions().get(0).getDescription().getValue(Language.EN));
    }

    @Test
    public void testSendingCreateCourseUnitMessage_missingSchemaVersion_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        String json = jsonMapper.writeValueAsString(req);

        Message responseMessage = jmsTemplate.sendAndReceive("handler", session -> {
            Message message = session.createTextMessage(json);
            message.setStringProperty(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_COURSEUNIT_REQUEST.name());
            message.setStringProperty(MessageHeader.JMS_XUSERID, organisingOrganisationId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            return message;
        });

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithInvalidLanguage_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        // first with valid language
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);
        courseUnit.setTeachingLanguage(Collections.singletonList("fi"));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        LocalisedString courseUnit2Name = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        // then with bogus language
        CourseUnitWriteDTO courseUnit2 = DtoInitializer.getCreateCourseUnitRequestDTO("ID2", "RAIRAI", courseUnit2Name,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit2.setStudyElementPermanentId("PERMID2");
        courseUnit2.setStatus(StudyStatus.ACTIVE);
        courseUnit2.setTeachingLanguage(Collections.singletonList("raipatirai"));

        req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit2));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse jsonValidationFailedResponse = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(jsonValidationFailedResponse.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateCourseUnitMessageV8WithInvalidLanguage_shouldSucceed() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        // test v8 api. teaching language should be set to empty because no valid language was given
        fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation organisationV8 = DtoInitializerV8.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationReference orgRefV8 = DtoInitializerV8.getOrganisationReference(organisationV8,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.OrganisationRole.ROLE_MAIN_ORGANIZER);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO courseUnitV8 = DtoInitializerV8.getCreateCourseUnitRequestDTO(
            "ID3", "RAIRAI", new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("test", null, null),
            Collections.singletonList(DtoInitializerV8.getCooperationNetwork(networkEntity.getId(), null,
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))),
            Collections.singletonList(orgRefV8), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnitV8.setTeachingLanguage(Collections.singletonList("pöö"));

        fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest courseUnitRequestV8 =
            new fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest();
        courseUnitRequestV8.setCourseUnits(Collections.singletonList(courseUnitV8));

        JmsHelper.setMessageSchemaVersion(8);
        OrganisationEntity organisationEntity = organisationService.findById("TUNI").get();
        organisationEntity.setSchemaVersion(8);
        organisationService.update(organisationEntity);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, courseUnitRequestV8, organisingOrganisationId);

        fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse v8response =
            (fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(v8response.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);

        CourseUnitEntity created = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId("ID3", "TUNI").get();
        assertEquals(Collections.emptyList(), created.getTeachingLanguage());
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithoutCooperationNetworks_shouldFail() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.emptyList(), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getErrors().stream().anyMatch( e -> e.contains("$.courseUnits[0].cooperationNetworks: must have at least 1 items but found 0")));

        courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            null, Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStudyElementPermanentId("PERMID1");
        courseUnit.setStatus(StudyStatus.CANCELLED);

        req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getErrors().stream().anyMatch(e -> e.contains("$.courseUnits[0]: required property 'cooperationNetworks' not found")));

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

        NetworkWriteDTO network = DtoInitializer.getNetwork("CN-1", new LocalisedString("Verkosto", null, null), validity, orgs);
        networkService.create(NetworkEntity.from(network));

        // TUNI already persisted in setUp()
        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);

        Organisation organisation2 = DtoInitializer.getOrganisation(organisingOrganisationId2, organisingOrganisationId2);
        OrganisationEntity organisationEntity2 = OrganisationEntity.from(organisation2);
        organisationEntity2.setId(organisingOrganisationId2);
        organisationEntity2.setQueue(organisingOrganisationId2);
        organisationEntity2.setNotificationsEnabled(true);
        organisationEntity2.setSchemaVersion(messageSchemaVersion);
        organisationService.create(organisationEntity2);

        Organisation organisation3 = DtoInitializer.getOrganisation(organisingOrganisationId3, organisingOrganisationId3);
        OrganisationEntity organisationEntity3 = OrganisationEntity.from(organisation3);
        organisationEntity3.setId(organisingOrganisationId3);
        organisationEntity3.setQueue(organisingOrganisationId3);
        organisationEntity3.setNotificationsEnabled(false); // NOTE: this one doesn't want notifications :)
        organisationEntity3.setSchemaVersion(messageSchemaVersion);
        organisationService.create(organisationEntity3);

        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
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
    public void testSendingUpdateCourseUnitMessage_notificationSentToUpdatedNetworks_shouldSucceed() throws JMSException {
        String organisingOrganisationId = "TUNI";
        String organisingOrganisationId2 = "SAV";

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

        List<NetworkOrganisation> orgsInNetwork1 = new ArrayList<>();
        orgsInNetwork1.add(networkOrganisation);

        List<NetworkOrganisation> orgsInNetwork2 = new ArrayList<>();
        orgsInNetwork2.add(networkOrganisation);
        orgsInNetwork2.add(networkOrganisation2);

        NetworkWriteDTO network1 = DtoInitializer.getNetwork("CN-1", new LocalisedString("Verkosto 1", null, null), validity, orgsInNetwork1);
        networkService.create(NetworkEntity.from(network1));

        NetworkWriteDTO network2 = DtoInitializer.getNetwork("CN-2", new LocalisedString("Verkosto 2", null, null), validity, orgsInNetwork2);
        networkService.create(NetworkEntity.from(network2));

        // TUNI already persisted in setUp()
        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);

        Organisation organisation2 = DtoInitializer.getOrganisation(organisingOrganisationId2, organisingOrganisationId2);
        OrganisationEntity organisationEntity2 = OrganisationEntity.from(organisation2);
        organisationEntity2.setId(organisingOrganisationId2);
        organisationEntity2.setQueue(organisingOrganisationId2);
        organisationEntity2.setNotificationsEnabled(true);
        organisationEntity2.setSchemaVersion(messageSchemaVersion);
        organisationService.create(organisationEntity2);

        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetwork1 = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto 1", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork cooperationNetwork2 = DtoInitializer.getCooperationNetwork(
            "CN-2", new LocalisedString("Verkosto 2", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName,
            Collections.singletonList(cooperationNetwork1), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStatus(StudyStatus.CANCELLED);

        CreateCourseUnitRequest createReq = new CreateCourseUnitRequest();
        createReq.setCourseUnits(Collections.singletonList(courseUnit));

        Message createResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createReq, organisingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(createResponseMessage);
        assertEquals(Status.OK, resp.getStatus());

        // check that message was not sent to sender of the original create message...
        Message notificationInOrganisation1Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId);
        assertNull(notificationInOrganisation1Queue);

        // ...and not sent to organisation2 since it's not part of cooperationNetwork1
        Message notificationInOrganisation2Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId2);
        assertNull(notificationInOrganisation2Queue);

        // now update the courseunit so that it includes cooperationNetwork2 too
        courseUnit.setCooperationNetworks(List.of(cooperationNetwork1, cooperationNetwork2));

        UpdateCourseUnitRequest updateReq = new UpdateCourseUnitRequest();
        updateReq.setCourseUnit(courseUnit);

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateReq, organisingOrganisationId);
        DefaultResponse updateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertEquals(Status.OK, updateResp.getStatus());

        // check that message was not sent to sender of the original create message...
        notificationInOrganisation1Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId);
        assertNull(notificationInOrganisation1Queue);

        // check if notification was received in organisation2...
        Message studyElementUpdatedMessage = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId2);
        assertNotNull(studyElementUpdatedMessage);

        CompositeIdentifiedEntityModifiedNotification notification = (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(studyElementUpdatedMessage);

        assertNotNull(notification);
        assertNotNull(notification.getTimestamp());
        assertEquals(notification.getSendingOrganisationTkCode(), organisingOrganisationId);

        // make sure it was in the "created" list as it was a "new" course unit in cooperationNetwork2
        assertEquals(1, notification.getCreated().size());
        assertTrue(CollectionUtils.isEmpty(notification.getUpdated()));
        assertTrue(CollectionUtils.isEmpty(notification.getDeleted()));

        assertEquals("ID1", notification.getCreated().get(0).getReferenceIdentifier());
        assertEquals(organisingOrganisationId, notification.getCreated().get(0).getReferenceOrganizer());

        // now update the courseunit so that it only includes cooperationNetwork2
        courseUnit.setCooperationNetworks(List.of(cooperationNetwork2));

        updateReq = new UpdateCourseUnitRequest();
        updateReq.setCourseUnit(courseUnit);

        updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateReq, organisingOrganisationId);
        updateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertEquals(Status.OK, updateResp.getStatus());

        // check that message was not sent to sender of the original create message...
        notificationInOrganisation1Queue = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId);
        assertNull(notificationInOrganisation1Queue);

        // check if notification was received in organisation2...
        studyElementUpdatedMessage = JmsHelper.receiveObject(jmsTemplate, organisingOrganisationId2);
        assertNotNull(studyElementUpdatedMessage);

        notification = (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(studyElementUpdatedMessage);

        assertNotNull(notification);
        assertNotNull(notification.getTimestamp());
        assertEquals(notification.getSendingOrganisationTkCode(), organisingOrganisationId);

        // make sure it was in the "updated" list this time in cooperationNetwork2
        assertTrue(CollectionUtils.isEmpty(notification.getCreated()));
        assertEquals(1, notification.getUpdated().size());
        assertTrue(CollectionUtils.isEmpty(notification.getDeleted()));

        assertEquals("ID1", notification.getUpdated().get(0).getReferenceIdentifier());
        assertEquals(organisingOrganisationId, notification.getUpdated().get(0).getReferenceOrganizer());
    }

    @Test
    public void testSendingCreateCourseUnitMessage_shouldSucceedCodeContainsSlash() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), LocalisedString.from(networkEntity.getName()), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAI/RAI", courseUnitName,
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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, createdCourseUnit.getType());

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(result.getStudyElementId(), result.getStudyElementId());
        assertEquals(result.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getCreatedTime());
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_shouldSucceed() throws JMSException, IOException, EntityNotFoundException {
        String organisingOrganisationId = "TUNI";
        persistNetworkEntity("CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, createdUnit.getType());
        assertEquals(StudyStatusEntity.ACTIVE, createdUnit.getStatus());
    }

    @Test
    public void testSendingCreateCourseUnitMessageAsJson_containsSpecialCharactersAndXss_shouldSucceedAndTagsRemoved() throws JMSException, IOException, EntityNotFoundException {
        String organisingOrganisationId = "TUNI";
        persistNetworkEntity("CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

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
        assertEquals("<b>test</b>Final Thesis", createdUnit.getName().getValue("en"));
        assertNotNull(createdUnit.getCreatedTime());
        assertNotNull(createdUnit.getCreatedTime());
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, createdUnit.getType());
        assertEquals(StudyStatusEntity.ACTIVE, createdUnit.getStatus());
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
    public void testSendingCreateCourseUnitMessageAsJson_shouldFailMessageHasTwoSameCourseUnits() throws JMSException, EntityNotFoundException {
        String organisingOrganisationId = "JUY";

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity(organisingOrganisationId, null, null, this.messageSchemaVersion);
        organisationService.create(organisationEntity);

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

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

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("ID1", null, realisationName,
            Collections.singletonList(studyElementReference), null, null);

        courseUnit.setRealisations(Arrays.asList(realisation));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        // Expected errors:
        //$.courseUnits[0].realisations: must be valid to one and only one schema, but 0 are valid
        //$.courseUnits[0].realisations: array found, null expected
        //$.courseUnits[0].realisations[0].organisationReferences: null found, array expected
        //$.courseUnits[0].realisations: must be valid to one and only one schema, but 0 are valid
        //$.courseUnits[0].realisations: array found, null expected
        //$.courseUnits[0].realisations[0].organisationReferences: null found, array expected
        //$.courseUnits[0].realisations[0].cooperationNetworks: null found, array expected.
        assertEquals(7, resp.getErrors().size());
    }

    @Test
    public void testSendingCreateCourseUnitMessageWithRealisations_shouldSuccess() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), LocalisedString.from(networkEntity.getName()), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("id1", "identifiercode", realisationName,
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
        assertEquals(StudyStatusEntity.ACTIVE, createdCourseUnit.getStatus());
        assertEquals(organisation.getOrganisationTkCode(), createdCourseUnit.getOrganizingOrganisationId());

        RealisationEntity createdRealisation = realisationService.findByIdAndOrganizingOrganisationId(
            realisation.getRealisationId(), organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation);
        assertEquals(1, createdRealisation.getStudyElementReferences().size());
        assertEquals(courseUnit.getStudyElementId(), createdRealisation.getStudyElementReferences().get(0).getReferenceIdentifier());
        assertEquals(createdCourseUnit.getOrganizingOrganisationId(), createdRealisation.getStudyElementReferences().get(0).getReferenceOrganizer());
        assertEquals(StudyElementTypeEntity.COURSE_UNIT, createdRealisation.getStudyElementReferences().get(0).getReferenceType());
        assertNotNull(createdRealisation.getCreatedTime());
        assertEquals(StudyStatusEntity.ACTIVE, createdRealisation.getStatus());

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
    public void testSendingCreateCourseUnitMessageWithRealisationThatHasMissingCooperationNetworks_shouldFail() throws JMSException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), LocalisedString.from(networkEntity.getName()), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode());

        LocalisedString realisationName = new LocalisedString("nimi", null, null);
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("id1", "identifiercode", realisationName,
            Collections.singletonList(studyElementReference), null, Arrays.asList(organisationReference));

        courseUnit.setRealisations(Arrays.asList(realisation));

        CreateCourseUnitRequest req = new CreateCourseUnitRequest();
        req.setCourseUnits(Collections.singletonList(courseUnit));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        realisation = DtoInitializer.getRealisation("id1", "identifiercode", realisationName,
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
            new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), LocalisedString.from(networkEntity.getName()), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("ID1", "RAIRAI", courseUnitName, Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));

        AssessmentItemWriteDTO assessmentItem = DtoInitializer.getAssessmentItem("AI-1", new LocalisedString("arvioinninkohde 1", null, null));

        AssessmentItemWriteDTO assessmentItem2 = DtoInitializer.getAssessmentItem("AI-2", null);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItem.getAssessmentItemId());

        StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItem2.getAssessmentItemId());

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("ID1", "IDENTIFIERCODE", new LocalisedString("nimi", null, null),
            Collections.singletonList(studyElementReference), Collections.singletonList(network), Arrays.asList(organisationReference));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        RealisationWriteDTO realisation2 = DtoInitializer.getRealisation("ID2", "IDENTIFIERCODE2", new LocalisedString("nimi2", null, null),
            Collections.singletonList(studyElementReference2), Collections.singletonList(network), Arrays.asList(organisationReference));
        realisation2.setStartDate(LocalDate.now().minusMonths(2));
        realisation2.setEndDate(LocalDate.now().plusMonths(2));
        realisation2.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(20));
        realisation2.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(20));

        assessmentItem.setRealisations(Collections.singletonList(realisation));
        assessmentItem2.setRealisations(Collections.singletonList(realisation2));
        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO-1", "kuvaus", Collections.singletonList(assessmentItem),
            new LocalisedString("suoritustapa 1", null, null));

        CompletionOptionWriteDTO completionOption2 = DtoInitializer.getCompletionOption("CO-2", "kuvaus", Collections.singletonList(assessmentItem2), null);

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
        assertEquals(StudyStatusEntity.ACTIVE, createdCourseUnit.getStatus());
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
            sr.getReferenceType().equals(StudyElementTypeEntity.ASSESSMENT_ITEM)
                && sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())
                && sr.getReferenceAssessmentItemId().equals(assessmentItem.getAssessmentItemId())));
        assertNotNull(createdRealisation.getCreatedTime());
        assertEquals(StudyStatusEntity.ACTIVE, createdRealisation.getStatus());

        RealisationEntity createdRealisation2 = realisationService.findByIdAndOrganizingOrganisationId(realisation2.getRealisationId(),
            organisingOrganisationId).orElse(null);
        assertNotNull(createdRealisation2);
        assertEquals(1, createdRealisation2.getStudyElementReferences().size());
        assertTrue(createdRealisation2.getStudyElementReferences().stream().anyMatch(sr ->
            sr.getReferenceType().equals(StudyElementTypeEntity.ASSESSMENT_ITEM)
                && sr.getReferenceIdentifier().equals(courseUnit.getStudyElementId())
                && sr.getReferenceAssessmentItemId().equals(assessmentItem2.getAssessmentItemId())));

        assertNotNull(createdRealisation2.getCreatedTime());
        assertEquals(StudyStatusEntity.ACTIVE, createdRealisation2.getStatus());

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
    public void testSendingCreateCourseUnitMessageAsJson_shouldFailJsonHasUnknownFields() throws JMSException, IOException, EntityNotFoundException {
        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, createCourseUnitWithUnknownFieldsJson, MessageType.CREATE_COURSEUNIT_REQUEST.name(), "JUY");

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.FAILED);

        CourseUnitEntity createdUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            "78780", "JUY").orElse(null);
        assertNull(createdUnit);
    }

    // TODO: move to a separate service layer integration test someday?
    @Test
    public void testCreatingDuplicateCourseUnits_shouldFail() {
        OrganisationDataEntity org = EntityInitializer.getOrganisation("TESTORG", "TESTORG");
        OrganisationReferenceEntity organisationRef = EntityInitializer.getOrganisationReference(org, OrganisationRoleEntity.ROLE_MAIN_ORGANIZER);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("TESTORG", "blaa", new LocalisedStringEntity("test", null, null), 8);
        organisationService.create(organisationEntity);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("TESTID", "TESTORG", null, null);
        courseUnitEntity.setOrganisationReferences(Collections.singletonList(organisationRef));

        courseUnitService.create(courseUnitEntity);
        courseUnitEntity.setId(null);
        assertThrows(CreateFailedException.class, () -> courseUnitService.create(courseUnitEntity));
    }

    private final String createCourseUnitJson =
        """
            {
                "courseUnits": [
                    {
                        "studyElementId": "78780",
                        "studyElementIdentifierCode": "4SAVYONT",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "validityStartDate": "2019-06-06",
                        "personReferences": [
                           {
                               "personRole": {
                                   "key": "asd",
                                   "codeSetKey": "dasd"
                               },
                               "person": {
                                   "homeEppn": "1234",
                                   "hostEppn": null,
                                   "firstNames": "Jaska",
                                   "givenName": "J",
                                   "surName": "Jokunen"
                               },
                               "definition": ""
                           }
                        ],            "sendingTime": "2019-06-06T21:00:00.000+03:00",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "OpinnÃ¤ytetyÃ¶",
                                "en": "Final Thesis",
                                "sv": null
                            }
                        },
                        "learningMaterials": null,
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
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
                        "organisationReferences": [
                            {
            					 "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "TUNI",
                                    "organisationTkCode": "TUNI"
                                }
                            }
                        ],
                        "keywords": [
                            {
                                "key": "testkey",
                                "value": {
                                    "values": {
                                        "fi": "value fi",
                                        "en": "value en",
                                        "sv": "value sv"
                                    }
                                },
                                "keySet": "testkeyset"
                            }
                        ]        }
                ]
            }
        """;

    private final String createCourseUnitJsonWitSpecialCharacters =
        """
            {
                "courseUnits": [
                    {
                        "studyElementId": "78780",
                        "studyElementIdentifierCode": "4SAVYONT",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "validityStartDate": "2019-06-06",
                        "personReferences": [
                           {
                               "personRole": {
                                   "key": "asd",
                                   "codeSetKey": "dasd"
                               },
                               "person": {
                                   "homeEppn": "1234",
                                   "hostEppn": null,
                                   "firstNames": "Jaska",
                                   "givenName": "J",
                                   "surName": "Jokunen"
                               },
                               "definition": ""
                           }
                        ],            "sendingTime": "2019-06-06T21:00:00.000+03:00",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "<xc",
                                "en": "<b>test</b>Final Thesis",
                                "sv": "<script type='text/javascript'>xss test</script>"
                            }
                        },
                        "learningMaterials": null,
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
                        "cooperationNetworks": [
                            {
                                "id": "CN-1",
                                "name": {
                                    "values": {
                                        "fi": "Verkosto 1 < dsac > <> #!#Â¤Â¤&%&/(@",
                                        "en": "Network 1",
                                        "sv": null
                                    }
                                },
                                "enrollable": true
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
                        "keywords": [
                            {
                                "key": "testkey",
                                "value": {
                                    "values": {
                                        "fi": "value fi",
                                        "en": "value en",
                                        "sv": "value sv"
                                    }
                                },
                                "keySet": "testkeyset"
                            }
                        ]        }
                ]
            }
        """;

    private final String createCourseUnitJsonMissingCooperationNetwork =
        """
            {
                "courseUnits": [
                    {
                        "studyElementId": "78780",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "sendingTime": "2019-06-06T21:00:00.000Z",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "OpinnÃ¤ytetyÃ¶",
                                "en": "Final Thesis",
                                "sv": null
                            }
                        },
                        "learningMaterials": null,
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
                        "cooperationNetworks": [
                        ],
                        "organisationReferences": [
                            {
            					 "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "JUY",
                                    "organisationTkCode": "JUY"
                                }
                            }
                        ]
                    }
                ]
            }
        """;

    private final String createCourseUnitJsonTwoSameCourseUnits =
        """
            {
                "courseUnits": [
                    {
                        "studyElementId": "78780",
                        "studyElementIdentifierCode": "4SAVYONT",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "sendingTime": "2019-06-06T21:00:00.000Z",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "OpinnÃ¤ytetyÃ¶",
                                "en": "Final Thesis",
                                "sv": null
                            }
                        },
                        "learningMaterials": null,
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
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
                        "organisationReferences": [
                            {
            					 "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "JUY",
                                    "organisationTkCode": "JUY"
                                }
                            }
                        ]
                    },
                    {
                        "studyElementId": "78780",
                        "studyElementIdentifierCode": "4SAVYONT",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "sendingTime": "2019-06-06T21:00:00.000Z",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "OpinnÃ¤ytetyÃ¶",
                                "en": "Final Thesis",
                                "sv": null
                            }
                        },
                        "learningMaterials": null,
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
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
                        "organisationReferences": [
                            {
            					 "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "JUY",
                                    "organisationTkCode": "JUY"
                                }
                            }
                        ]
                    }
                ]
            }
        """;

    private final String createCourseUnitMissingMainlyResponsibleOrgRefJson =
        """
            {
            	"courseUnits": [
            		{
                       "status": "ACTIVE",
             			"studyElementId": "78780",
            			"studyElementIdentifierCode": "4SAVYONT",
            			"type": "COURSE_UNIT",
            			"abbreviation": "OpinetyÃ¶",
            			"validityStartDate": "2019-06-06",
                       "personReferences": [
                           {
                               "personRole": {
                                 "key": "asd",
                                 "codeSetKey": "dasd"
                               },
                               "person": {
                                  "homeEppn": "1234",
                                  "hostEppn": null,
                                  "firstNames": "Jaska",
                                  "givenName": "J",
                                  "surName": "Jokunen"
                               },
                               "definition": ""
                           }
                       ],			"sendingTime": "2019-06-06T21:00:00.000+03:00",
            			"teachingLanguage": [
            				"en"
            			],
                       "assessmentScale": 1,
            			"name": {
            				"values": {
            					"fi": "OpinnÃ¤ytetyÃ¶",
            					"en": "Final Thesis",
            					"sv": null
            				}
            			},
            			"learningMaterials": null,
            			"groupSize": 122,
            			"completionOptions": [
            				{
            					"completionOptionId": "yfIE",
            					"description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
            				}
            			],
            			"creditsMin": 15,
            			"creditsMax": 35,
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
            			"organisationReferences": [
            				{
            					"organisationRole": 2,
            					"target": {
            						"organisationIdentifier": "JUY",
            						"organisationTkCode": "JUY"
            					}
            				}
            			]
            		}
            	]
            }
        """;

    private final String createCourseUnitWithUnknownFieldsJson =
        """
            {
                "courseUnits": [
                    {
                        "studyElementId": "78780",
                        "studyElementIdentifierCode": "4SAVYONT",
                        "type": "COURSE_UNIT",
                        "abbreviation": "OpinetyÃ¶",
                        "validityStartDate": "2019-06-06",
                        "personReferences": [
                           {
                               "personRole": {
                                   "key": "asd",
                                   "codeSetKey": "dasd"
                               },
                               "person": {
                                   "homeEppn": "1234",
                                   "hostEppn": null,
                                   "firstNames": "Jaska",
                                   "givenName": "J",
                                   "surName": "Jokunen"
                               },
                               "definition": ""
                           }
                        ],
                        "sendingTime": "2019-06-06T21:00:00.000+03:00",
                        "teachingLanguage": [
                            "en"
                        ],
                        "assessmentScale": 1,
                        "name": {
                            "values": {
                                "fi": "OpinnÃ¤ytetyÃ¶",
                                "en": "Final Thesis",
                                "sv": null
                            }
                        },
                        "studyElementStatus": "RANDOM_VALUE",
%s
%s
                        "groupSize": 122,
                        "completionOptions": [
                            {
                                "completionOptionId": "yfIE",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti"
                            }
                        ],
                        "creditsMin": 15,
                        "creditsMax": 35,
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
                        "organisationReferences": [
                            {
                                "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "JUY",
                                    "organisationTkCode": "JUY"
                                }
                            }
                        ]
                    }
                ]
            }
        """.formatted(// unknown field
            "            \"anotherRandomField\": \"RANDOM_VALUE2\",", // unknown field
            "            \"learningMaterials\": null,\n");
}
