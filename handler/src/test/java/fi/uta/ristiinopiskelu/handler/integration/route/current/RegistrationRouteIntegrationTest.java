package fi.uta.ristiinopiskelu.handler.integration.route.current;

import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.*;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
import fi.uta.ristiinopiskelu.messaging.util.OidPersonNode;
import fi.uta.ristiinopiskelu.persistence.repository.*;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
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
public class RegistrationRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    private NetworkEntity testNetwork;
    private OrganisationEntity sendingOrganisation;
    private OrganisationEntity receivingOrganisation;

    @BeforeEach
    public void before() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        ValidityEntity validity = new ValidityEntity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisationEntity organisation = new NetworkOrganisationEntity();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisationEntity organisation2 = new NetworkOrganisationEntity();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        testNetwork = new NetworkEntity();
        testNetwork.setId("CN-1");
        testNetwork.setName(new LocalisedStringEntity("verkosto", null, null));
        testNetwork.setOrganisations(Arrays.asList(organisation, organisation2));
        testNetwork.setValidity(validity);
        testNetwork.setPublished(true);
        networkRepository.create(testNetwork);

        sendingOrganisation = new OrganisationEntity();
        sendingOrganisation.setOrganisationName(new LocalisedStringEntity("Lähettävä testiorganisaatio", null, null));
        sendingOrganisation.setId("TESTORG1");
        sendingOrganisation.setQueue("testiorganisaatio1");
        sendingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        receivingOrganisation = new OrganisationEntity();
        receivingOrganisation.setOrganisationName(new LocalisedStringEntity("Vastaanottava testiorganisaatio", null, null));
        receivingOrganisation.setId("TESTORG2");
        receivingOrganisation.setQueue("testiorganisaatio2");
        receivingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessage_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        assertInstanceOf(BytesMessage.class, responseMessage);
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        assertInstanceOf(TextMessage.class, messageReceivedInOrganisation);
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        Rank selectionRank = new Rank(1,10,3,30);
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            RegistrationReplySelection replySelection = RegistrationReplySelection.fromCreate(sel);
            replySelection.setRank(selectionRank);
            replySelections.add(replySelection);

        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setSelections(replySelections);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());

        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(req.getStudent().getHomeStudentNumber(), reg.getStudent().getHomeStudentNumber());
        assertEquals(replyRequest.getHostStudentNumber(), reg.getStudent().getHostStudentNumber());
        assertStudyRightEquals(req.getStudent().getHomeStudyRight(), reg.getStudent().getHomeStudyRight());
        assertStudyRightEquals(replyRequest.getHostStudyRight(), reg.getStudent().getHostStudyRight());
        assertEquals(RegistrationStatusEntity.REGISTERED, reg.getStatus());

        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatusEntity.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> rankMatches(selectionRank, s.getRank())));
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessage_withInvalidStudentAddressCountryCode_shouldFail() throws JMSException {
        String jsonContent = """
            {
              "sendingOrganisationTkCode" : "TESTORG1",
              "receivingOrganisationTkCode" : "TESTORG2",
              "student" : {
                "homeEppn" : "masa@tuni.fi",
                "hostEppn" : null,
                "firstNames" : "Matti",
                "surName" : "Nykänen",
                "givenName" : "Matti",
                "oid" : "1.2.246.562.24.35312849665",
                "personId" : "230976-156R",
                "homeStudentNumber" : "1234-1234",
                "hostStudentNumber" : null,
                "hostStudyRight" : null,
                "homeStudyRight" : {
                  "identifiers" : {
                    "organisationTkCodeReference" : "TESTORG1",
                    "studyRightId" : "OPISKOIK1"
                  },
                  "studyRightStatus" : {
                    "studyRightStatusValue" : 1,
                    "startDate" : "2017-01-01",
                    "endDate" : "2020-06-01"
                  },
                  "studyRightType" : null,
                  "keywords" : [ {
                    "key" : "testkey",
                    "value" : {
                      "values" : {
                        "fi" : "value fi",
                        "en" : "value en",
                        "sv" : "value sv"
                      }
                    },
                    "keySet" : "testKeySet",
                    "keySetValue" : null
                  } ]
                },
                "dateOfBirth" : "1950-01-01",
                "gender" : 1,
                "addresses" : [ {
                  "street" : "Kotikatu 1",
                  "postalCode" : "33100",
                  "postOffice" : "Peräkylä",
                  "country" : "2342"
                } ],
                "countryOfCitizenship" : null,
                "municipalityOfResidence" : null,
                "motherTongue" : "fi",
                "preferredLanguage" : null,
                "email" : null,
                "phone" : null,
                "safetyProhibition" : null
              },
              "networkIdentifier" : "CN-1",
              "networkDescription" : null,
              "selections" : [ {
                "selectionItemId" : "OJ1",
                "selectionItemType" : "COURSE_UNIT",
                "selectionItemStatus" : 1,
                "selectionItemStatusInfo" : null,
                "parent" : null,
                "subGroupSelections" : null,
                "rank" : null
              } ],
              "enrolmentDateTime" : "2024-01-12T13:07:35.222+02:00",
              "warnings" : null,
              "descriptions" : null
            }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, jsonContent, MessageType.CREATE_REGISTRATION_REQUEST.name(), sendingOrganisation.getId());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains("$.student.addresses[0].country: does not have a value in the enumeration"));
    }

    @Test
    public void testSendingCourseUnitRegistrationWithInvalidAndValidMothertongues() throws JMSException {

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));
        courseUnitRepository.create(courseUnitEntity);

        String jsonContentTemplate = """
            {
              "sendingOrganisationTkCode" : "TESTORG1",
              "receivingOrganisationTkCode" : "TESTORG2",
              "student" : {
                "oid" : "%s",
                "homeStudyRight" : {
                  "identifiers" : {
                    "organisationTkCodeReference" : "TESTORG1",
                    "studyRightId" : "OPISKOIK1"
                  },
                  "studyRightStatus" : {
                    "studyRightStatusValue" : 1,
                    "startDate" : "2017-01-01",
                    "endDate" : "2020-06-01"
                  }
                },
                "dateOfBirth" : "1950-01-01",
                "gender" : 1,
                "countryOfCitizenship" : null,
                "motherTongue" : %s
              },
              "networkIdentifier" : "CN-1",
              "selections" : [ {
                "selectionItemId" : "OJ1",
                "selectionItemType" : "COURSE_UNIT",
                "selectionItemStatus" : 1
              } ],
              "enrolmentDateTime" : "%s"
            }
            """;

        String jsonContentNull = jsonContentTemplate.formatted(Oid.randomOid(OidPersonNode.PERSON),
            "null", OffsetDateTime.now().toString());
        String jsonContentEmpty = jsonContentTemplate.formatted(Oid.randomOid(OidPersonNode.PERSON),
            "", OffsetDateTime.now().toString());

        // Change these after it is decided what motherTongue should actually contain instead of free text
        //String jsonContentValid = jsonContentTemplate.formatted("FI", OffsetDateTime.now().toString());
        String jsonContentProbablyNotValidInFuture = jsonContentTemplate.formatted(Oid.randomOid(OidPersonNode.PERSON),
            "\"Suomi\"", OffsetDateTime.now().toString());

        Message responseMessageNull = JmsHelper.sendAndReceiveJson(jmsTemplate, jsonContentNull, MessageType.CREATE_REGISTRATION_REQUEST.name(), sendingOrganisation.getId());
        Object respNull = jmsTemplate.getMessageConverter().fromMessage(responseMessageNull);
        assertInstanceOf(JsonValidationFailedResponse.class, respNull);

        Message responseMessageEmpty = JmsHelper.sendAndReceiveJson(jmsTemplate, jsonContentEmpty, MessageType.CREATE_REGISTRATION_REQUEST.name(), sendingOrganisation.getId());
        Object respEmpty = jmsTemplate.getMessageConverter().fromMessage(responseMessageEmpty);
        assertInstanceOf(JsonValidationFailedResponse.class, respEmpty);

        Message responseMessageOk = JmsHelper.sendAndReceiveJson(jmsTemplate, jsonContentProbablyNotValidInFuture, MessageType.CREATE_REGISTRATION_REQUEST.name(), sendingOrganisation.getId());
        Object respOk = jmsTemplate.getMessageConverter().fromMessage(responseMessageOk);
        assertInstanceOf(RegistrationResponse.class, respOk);
        assertEquals(Status.OK, ((RegistrationResponse)respOk).getStatus());
    }

    @Test
    public void testSendingCourseUnitRegistration_registrationRequestIdMatchesEntityId_shouldSucceed() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).toList();
        assertEquals(1, registrations.size());
        assertEquals(registrations.get(0).getId(), resp.getRegistrationRequestId());
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessageWithoutStudentEppn_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        Rank selectionRank = new Rank(1,10,3,30);
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            RegistrationReplySelection replySelection = RegistrationReplySelection.fromCreate(sel);
            replySelection.setRank(selectionRank);
            replySelections.add(replySelection);

        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setSelections(replySelections);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setHostStudentEppn(null);

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());

        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(req.getStudent().getHomeStudentNumber(), reg.getStudent().getHomeStudentNumber());
        assertEquals(replyRequest.getHostStudentNumber(), reg.getStudent().getHostStudentNumber());
        assertStudyRightEquals(req.getStudent().getHomeStudyRight(), reg.getStudent().getHomeStudyRight());
        assertStudyRightEquals(replyRequest.getHostStudyRight(), reg.getStudent().getHostStudyRight());
        assertEquals(RegistrationStatusEntity.REGISTERED, reg.getStatus());

        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatusEntity.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> rankMatches(selectionRank, s.getRank())));

    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessageWithoutStudentnumber_shouldFail() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        Rank selectionRank = new Rank(1, 10, 3, 30);
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for (CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            RegistrationReplySelection replySelection = RegistrationReplySelection.fromCreate(sel);
            replySelection.setRank(selectionRank);
            replySelections.add(replySelection);

        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setSelections(replySelections);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setHostStudentNumber(null);

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());

        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.FAILED);
    }
    @Test
    public void testSendingCourseUnitRegistrationStatusMessageWithoutStydyright_shouldFail() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        Rank selectionRank = new Rank(1, 10, 3, 30);
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for (CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            RegistrationReplySelection replySelection = RegistrationReplySelection.fromCreate(sel);
            replySelection.setRank(selectionRank);
            replySelections.add(replySelection);

        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setSelections(replySelections);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setHostStudyRight(null);

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());

        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.FAILED);
    }
    @Test
    public void testSendingCourseUnitRegistrationStatusMessageRejected_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // Reject registration for selected request
        Rank selectionRank = new Rank(1, 10, 3, 30);
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for (CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.REJECTED);
            sel.setSelectionItemStatusInfo("Ilmoittautuminen hylätty.");
            RegistrationReplySelection replySelection = RegistrationReplySelection.fromCreate(sel);
            replySelection.setRank(selectionRank);
            replySelections.add(replySelection);

        }

        RegistrationReplyRequest replyRequest = new RegistrationReplyRequest();
        replyRequest.setSelections(replySelections);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setStatus(RegistrationStatus.REGISTERED);

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());

        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.OK);
    }

    @Test
    public void testSendingCourseUnitRegistrationWithPreviousSchemaVersion_shouldSucceed() throws JMSException, IOException {
        int previousSchemaVersion = this.messageSchemaVersion - 1;
        JmsHelper.setMessageSchemaVersion(previousSchemaVersion);

        sendingOrganisation = organisationRepository.findById(sendingOrganisation.getId()).get();
        sendingOrganisation.setSchemaVersion(previousSchemaVersion);
        organisationRepository.update(sendingOrganisation);

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus.PENDING);

        fi.uta.ristiinopiskelu.messaging.message.v8.registration.CreateRegistrationRequest req =
            MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.v8.Description description = new fi.uta.ristiinopiskelu.datamodel.dto.v8.Description();
        description.setKey("test");
        description.setName(new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("jee", null, null));
        description.setValue(new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("jee", null, null));

        req.setDescriptions(Collections.singletonList(description));
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        fi.uta.ristiinopiskelu.messaging.message.v8.registration.RegistrationResponse resp =
            (fi.uta.ristiinopiskelu.messaging.message.v8.registration.RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);
        assertEquals(description.getKey(), reg.getDescriptions().get(0).getKey());
        assertEquals(description.getName().getValue("fi"), reg.getDescriptions().get(0).getName().getValue("fi"));
        assertEquals(description.getValue().getValue("fi"), reg.getDescriptions().get(0).getValue().getValue("fi"));
    }

    @Test
    public void testSendingRealisationRegistration_shouldSuccess() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
            Arrays.asList(
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, List.of("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        CreateRegistrationSelectionParent requestCourseUnitSelection = req.getSelections().get(0).getParent();
        CreateRegistrationSelectionParent receivedCourseUnitSelection = receivedRequest.getSelections().get(0).getParent();

        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(requestCourseUnitSelection.getSelectionItemId(), receivedCourseUnitSelection.getSelectionItemId());
        assertEquals(requestCourseUnitSelection.getSelectionItemType(), receivedCourseUnitSelection.getSelectionItemType());

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(RegistrationReplySelection.fromCreate(receivedRequest.getSelections()));

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(req.getStudent().getHomeStudentNumber(), reg.getStudent().getHomeStudentNumber());
        assertEquals(replyRequest.getHostStudentNumber(), reg.getStudent().getHostStudentNumber());
        assertStudyRightEquals(req.getStudent().getHomeStudyRight(), reg.getStudent().getHomeStudyRight());
        assertStudyRightEquals(replyRequest.getHostStudyRight(), reg.getStudent().getHostStudyRight());
        assertEquals(RegistrationStatusEntity.REGISTERED, reg.getStatus());

        CreateRegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        RegistrationReplySelectionEntity regRealisationSelection = reg.getSelectionsReplies().get(0);
        CreateRegistrationSelectionParent replyCourseUnitSelection = req.getSelections().get(0).getParent();
        CreateRegistrationSelectionParentEntity regCourseUnitSelection = reg.getSelections().get(0).getParent();

        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.REALISATION, regRealisationSelection.getSelectionItemType());
        assertEquals(RegistrationSelectionItemStatusEntity.ACCEPTED, regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.COURSE_UNIT, regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingRealisationRegistrationWithOutdatedSelection_shouldFail() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
            Arrays.asList(
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingRealisationRegistrationWithOutdatedSelectionAndStatusAbortedByStudent_shouldSucceed() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
            Arrays.asList(
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ABORTED_BY_STUDENT, courseUnitParent, List.of("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));
    }

    @Test
    public void testSendingAssessmentItemRealisationRegistration_shouldSuccess() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("AI1");

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId("CO1");
        completionOptionEntity.setAssessmentItems(List.of(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithCompletionOptions("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null), Collections.singletonList(completionOptionEntity));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        CreateRegistrationSelectionParent completionOptionParent = DtoInitializer.getRegistrationSelectionCompletionOptionParent(
                completionOptionEntity.getCompletionOptionId(), courseUnitParent);

        CreateRegistrationSelectionParent assessmentItemParent = DtoInitializer.getRegistrationSelectionAssessmentItemParent(
                assessmentItemEntity.getAssessmentItemId(), completionOptionParent);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));

        CreateRegistrationSelection requestedSelection = req.getSelections().get(0);
        CreateRegistrationSelection receivedRealisationSelection = receivedRequest.getSelections().get(0);
        assertEquals(requestedSelection.getSelectionItemId(), receivedRealisationSelection.getSelectionItemId());
        assertEquals(requestedSelection.getSelectionItemType(), receivedRealisationSelection.getSelectionItemType());

        CreateRegistrationSelectionParent assessmentItemSelection = requestedSelection.getParent();
        CreateRegistrationSelectionParent receivedAssessmentItemSelection = receivedRealisationSelection.getParent();
        assertEquals(assessmentItemSelection.getSelectionItemId(), receivedAssessmentItemSelection.getSelectionItemId());
        assertEquals(assessmentItemSelection.getSelectionItemType(), receivedAssessmentItemSelection.getSelectionItemType());

        CreateRegistrationSelectionParent completionOptionSelection = assessmentItemSelection.getParent();
        CreateRegistrationSelectionParent receivedCompletionOptionSelection = receivedAssessmentItemSelection.getParent();
        assertEquals(completionOptionSelection.getSelectionItemId(), receivedCompletionOptionSelection.getSelectionItemId());
        assertEquals(completionOptionSelection.getSelectionItemType(), receivedCompletionOptionSelection.getSelectionItemType());

        CreateRegistrationSelectionParent courseUnitSelection = completionOptionSelection.getParent();
        CreateRegistrationSelectionParent receivedCourseUnitSelection = receivedCompletionOptionSelection.getParent();
        assertEquals(courseUnitSelection.getSelectionItemId(), receivedCourseUnitSelection.getSelectionItemId());
        assertEquals(courseUnitSelection.getSelectionItemType(), receivedCourseUnitSelection.getSelectionItemType());

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(RegistrationReplySelection.fromCreate(receivedRequest.getSelections()));

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(req.getStudent().getHomeStudentNumber(), reg.getStudent().getHomeStudentNumber());
        assertEquals(replyRequest.getHostStudentNumber(), reg.getStudent().getHostStudentNumber());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(), reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(req.getStudent().getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue().name(),
            reg.getStudent().getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue().name());
        assertEquals(req.getStudent().getHomeStudyRight().getStudyRightStatus().getStartDate(),
            reg.getStudent().getHomeStudyRight().getStudyRightStatus().getStartDate());
        assertEquals(req.getStudent().getHomeStudyRight().getStudyRightStatus().getEndDate(),
            reg.getStudent().getHomeStudyRight().getStudyRightStatus().getEndDate());
        assertEquals(req.getStudent().getHomeStudyRight().getStudyRightType() == null ? null : req.getStudent().getHomeStudyRight().getStudyRightType().name(),
            reg.getStudent().getHomeStudyRight().getStudyRightType() == null ? null : reg.getStudent().getHomeStudyRight().getStudyRightType().name());
        assertStudyRightEquals(replyRequest.getHostStudyRight(), reg.getStudent().getHostStudyRight());
        assertEquals(RegistrationStatusEntity.REGISTERED, reg.getStatus());

        CreateRegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        RegistrationReplySelectionEntity regRealisationSelection = reg.getSelectionsReplies().get(0);
        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.REALISATION, regRealisationSelection.getSelectionItemType());
        assertEquals(RegistrationSelectionItemStatusEntity.ACCEPTED, regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        CreateRegistrationSelectionParent replyAssessmentItemSelection = replyRealisationSelection.getParent();
        CreateRegistrationSelectionParentEntity regAssessmentItemSelection = reg.getSelections().get(0).getParent();
        assertEquals(replyAssessmentItemSelection.getSelectionItemId(), regAssessmentItemSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.ASSESSMENT_ITEM, regAssessmentItemSelection.getSelectionItemType());

        CreateRegistrationSelectionParent replyCompletionOptionSelection = replyAssessmentItemSelection.getParent();
        CreateRegistrationSelectionParentEntity regCompletionOptionSelection = regAssessmentItemSelection.getParent();
        assertEquals(replyCompletionOptionSelection.getSelectionItemId(), regCompletionOptionSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.COMPLETION_OPTION, regCompletionOptionSelection.getSelectionItemType());

        CreateRegistrationSelectionParent replyCourseUnitSelection = replyCompletionOptionSelection.getParent();
        CreateRegistrationSelectionParentEntity regCourseUnitSelection = regCompletionOptionSelection.getParent();
        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.COURSE_UNIT, regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingAssessmentItemRealisationRegistrationAborted_withoutCompletionOptionSelectionItemId_shouldSucceed() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("AI1");

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId("CO1");
        completionOptionEntity.setAssessmentItems(List.of(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithCompletionOptions("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null), Collections.singletonList(completionOptionEntity));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        // leave the completion option id out, should pass when ABORTED_BY_STUDENT is sent
        CreateRegistrationSelectionParent completionOptionParent = DtoInitializer.getRegistrationSelectionCompletionOptionParent(
                null, courseUnitParent);

        CreateRegistrationSelectionParent assessmentItemParent = DtoInitializer.getRegistrationSelectionAssessmentItemParent(
            assessmentItemEntity.getAssessmentItemId(), completionOptionParent);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ABORTED_BY_STUDENT, assessmentItemParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));
    }

    @Test
    public void testSendingRealisationWithGroupSelectionsRegistration_shouldSuccess() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
                        Arrays.asList(
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(realisationEntity.getRealisationId(),
                RegistrationSelectionItemStatus.PENDING, courseUnitParent, Collections.singletonList("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        CreateRegistrationSelection requestSelection = req.getSelections().get(0);
        CreateRegistrationSelection receivedSelection = receivedRequest.getSelections().get(0);
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(requestSelection.getSelectionItemId(), receivedSelection.getSelectionItemId());
        assertEquals(requestSelection.getSelectionItemType(), receivedSelection.getSelectionItemType());

        assertEquals(requestSelection.getParent().getSelectionItemId(), receivedSelection.getParent().getSelectionItemId());
        assertEquals(requestSelection.getParent().getSelectionItemType(), receivedSelection.getParent().getSelectionItemType());

        assertEquals(requestSelection.getSubGroupSelections().get(0), receivedSelection.getSubGroupSelections().get(0));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(RegistrationReplySelection.fromCreate(receivedRequest.getSelections()));

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(req.getStudent().getHomeStudentNumber(), reg.getStudent().getHomeStudentNumber());
        assertEquals(replyRequest.getHostStudentNumber(), reg.getStudent().getHostStudentNumber());
        assertStudyRightEquals(req.getStudent().getHomeStudyRight(), reg.getStudent().getHomeStudyRight());
        assertStudyRightEquals(replyRequest.getHostStudyRight(), reg.getStudent().getHostStudyRight());
        assertEquals(RegistrationStatusEntity.REGISTERED, reg.getStatus());

        CreateRegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        RegistrationReplySelectionEntity regRealisationSelection = reg.getSelectionsReplies().get(0);
        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.REALISATION, regRealisationSelection.getSelectionItemType());
        assertEquals(RegistrationSelectionItemStatusEntity.ACCEPTED, regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        CreateRegistrationSelectionParent replyCourseUnitSelection = req.getSelections().get(0).getParent();
        CreateRegistrationSelectionParentEntity regCourseUnitSelection = reg.getSelections().get(0).getParent();
        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(RegistrationSelectionItemTypeEntity.COURSE_UNIT, regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingRealisationRegistrationWithChooseManyGroups_shouldSucceed() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_MANY,
                        Arrays.asList(
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        // CHOOSE_MANY was redefined in October 2024 to mean 0-N group registrations, i.e., anything goes.

        // first test without selections (this used to fail with old definition of 1-N)
        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, null);
        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse registrationResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(registrationResponse.getStatus() == Status.OK);

        // now test with a single selection (this succeeded also previously)
        selection.setSubGroupSelections(List.of("TESTIRYHMA1"));
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        registrationResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(registrationResponse.getStatus() == Status.OK);

        // and finally test with multiple selections
        selection.setSubGroupSelections(Arrays.asList("TESTIRYHMA1", "TESTIRYHMA2"));
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        registrationResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(registrationResponse.getStatus() == Status.OK);
    }

    @Test
    public void testSendingRealisationRegistrationWithChooseOneGroups() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
                        Arrays.asList(
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                        new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());
        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, null);

        // CHOOSE_ONE was redefined in October 2024 to mean 1-N group registrations, i.e., at least one is required but more is allowed.

        // first test with no selections (must fail)
        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        // then test with one selection (the case that was previously required)
        selection.setSubGroupSelections(List.of("TESTIRYHMA1"));
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // finally test with several selections (this used to fail, now should succeed)
        selection.setSubGroupSelections(Arrays.asList("TESTIRYHMA1", "TESTIRYHMA2"));
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
    }

    @Test
    public void testSendingRealisationRegistrationAbortionWithMissingGroups_shouldSucceed() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        SelectionEntity groupSelection = EntityInitializer.getSelection(new LocalisedStringEntity("Opetusryhmä 1", null, null), SelectionTypeEntity.CHOOSE_ONE,
                Arrays.asList(
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Tentti", null, null), "TESTIRYHMA1", null),
                new SelectionValueEntity(null, null, new LocalisedStringEntity("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());
        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ABORTED_BY_STUDENT, courseUnitParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
    }

    @Test
    public void testSendingAssessmentItemRealisationRegistration_shouldFailAssessmentItemNotInCourseUnit() throws JMSException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("AI1");

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId("CO1");
        completionOptionEntity.setAssessmentItems(List.of(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithCompletionOptions("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null), Collections.singletonList(completionOptionEntity));

        courseUnitRepository.create(courseUnitEntity);

        StudyElementReferenceEntity ref = EntityInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        realisationRepository.create(realisationEntity);
        
        CreateRegistrationSelectionParent courseUnitParent = DtoInitializer.getRegistrationSelectionCourseUnitParent(
                courseUnitEntity.getStudyElementId());

        CreateRegistrationSelectionParent completionOptionParent = DtoInitializer.getRegistrationSelectionCompletionOptionParent(
                completionOptionEntity.getCompletionOptionId(), courseUnitParent);

        // "A2" is not found in courseunit.completionOptions.assessmentItems
        CreateRegistrationSelectionParent failingAssessmentItemParent = DtoInitializer.getRegistrationSelectionAssessmentItemParent(
                "AI2", completionOptionParent);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, failingAssessmentItemParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains("Registration has selected realisation that does not have assessment item reference to given course unit."));

        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(0, registrations.size());
    }

    @Test
    public void testSendingCourseUnitRegistrationMessage_shouldFailCourseUnitDoesNotBelongToGivenNetwork() throws JMSException, IOException {
        ValidityEntity validity = new ValidityEntity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkEntity wrongNetworkEntity = EntityInitializer.getNetworkEntity("DIFFERENT-NETWORKID-1", new LocalisedStringEntity("Väärä verkosto", null, null),
                testNetwork.getOrganisations(), validity, true);
        networkRepository.create(wrongNetworkEntity);

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));
        req.setNetworkIdentifier("DIFFERENT-NETWORKID-1");

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains("Request contains selection(s) that do not belong to or are not enrollable"));

        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(0, registrations.size());
    }

    @Test
    public void testSendingRegistrationWithoutStudentInfo_shouldFail() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setStudent(null);
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        req.setStudent(new ExtendedStudent());

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingRegistrationReceivedWithoutExtraInfo_shouldSuccess() throws JMSException, IOException {

        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelectionEntity selection = EntityInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatusEntity.PENDING);

        RegistrationEntity reg = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(), Collections.singletonList(selection), null,
            testNetwork.getId());
        reg = registrationRepository.create(reg);

        RegistrationReplyRequest req = new RegistrationReplyRequest();
        req.setRegistrationRequestId(reg.getId());
        req.setStatus(RegistrationStatus.RECEIVED);

        // extra field that should cause json schema validation error when sent with status = RECEIVED
        req.setHostStudentNumber("1234");

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, receivingOrganisation.getId());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        // remove the offending extra field
        req.setHostStudentNumber(null);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, receivingOrganisation.getId());
        DefaultResponse okResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(okResponse.getStatus() == Status.OK);
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessage_setStatusRejected_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        List<RegistrationReplySelection> replySelections = new ArrayList<>();
        for(CreateRegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            replySelections.add(RegistrationReplySelection.fromCreate(sel));
        }

        RegistrationReplyRequest replyRequest = new RegistrationReplyRequest();
        replyRequest.setSelections(replySelections);
        replyRequest.setStatus(RegistrationStatus.REGISTRATION_REJECTED);
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setRejectionReason(new LocalisedString("Ei saatu lisättyä", "Cant add", "nej"));

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);

        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);
        assertEquals(replyRequest.getRejectionReason().getValue("fi"), reg.getRejectionReason().getValue("fi"));
        assertEquals(replyRequest.getRejectionReason().getValue("en"), reg.getRejectionReason().getValue("en"));
        assertEquals(replyRequest.getRejectionReason().getValue("sv"), reg.getRejectionReason().getValue("sv"));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatusEntity.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        Message statusMessageReceivedInOrganisation = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedRegistrationReplyRequest receivedStatusMessage = (ForwardedRegistrationReplyRequest) jmsTemplate.getMessageConverter().fromMessage(statusMessageReceivedInOrganisation);

        assertEquals(replyRequest.getRejectionReason().getValue("fi"), receivedStatusMessage.getRejectionReason().getValue("fi"));
        assertEquals(replyRequest.getRejectionReason().getValue("en"), receivedStatusMessage.getRejectionReason().getValue("en"));
        assertEquals(replyRequest.getRejectionReason().getValue("sv"), receivedStatusMessage.getRejectionReason().getValue("sv"));

    }

    @Test
    public void testSendingCourseUnitRegistrationRequestWithoutStudentOidOrStudentPersonId_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        // send only personId
        req.getStudent().setPersonId(HetuUtil.generateRandom());
        req.getStudent().setOid(null);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        // send only oid
        req.getStudent().setPersonId(null);
        req.getStudent().setOid(Oid.randomOid(OidPersonNode.PERSON));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        // dont send either
        req.getStudent().setPersonId(null);
        req.getStudent().setOid(null);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        JsonValidationFailedResponse defaultResponse = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(defaultResponse.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCourseUnitRegistrationRequestWithoutStudentPhoneNumberDescription_shouldSucceed() throws JMSException, IOException {
        CooperationNetworkEntity cooperationNetwork = EntityInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new LocalisedStringEntity("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        CreateRegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Phone phone = new Phone();
        phone.setNumber("+3584012345678");

        // send only phone number
        req.getStudent().setPhone(Collections.singletonList(phone));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        // send both
        phone.setDescription("Testiä");
        req.getStudent().setPhone(Collections.singletonList(phone));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // dont send either
        phone.setNumber(null);
        phone.setDescription(null);
        req.getStudent().setPhone(Collections.singletonList(phone));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        JsonValidationFailedResponse response = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(response.getStatus() == Status.FAILED);

    }

    private void assertStudyRightEquals(StudyRight expected, StudyRightEntity actual) {
        if(expected == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertNotNull(expected.getIdentifiers());
        assertNotNull(actual.getIdentifiers());
        assertEquals(expected.getIdentifiers().getStudyRightId(), actual.getIdentifiers().getStudyRightId());
        assertEquals(expected.getIdentifiers().getOrganisationTkCodeReference(), actual.getIdentifiers().getOrganisationTkCodeReference());

        if(expected.getStudyRightStatus() == null) {
            assertNull(actual.getStudyRightStatus());
        } else {
            assertNotNull(actual.getStudyRightStatus());
            assertEquals(expected.getStudyRightStatus().getStudyRightStatusValue().name(), actual.getStudyRightStatus().getStudyRightStatusValue().name());
            assertEquals(expected.getStudyRightStatus().getStartDate(), actual.getStudyRightStatus().getStartDate());
            assertEquals(expected.getStudyRightStatus().getEndDate(), actual.getStudyRightStatus().getEndDate());
        }

        assertEquals(expected.getStudyRightType() == null ? null : expected.getStudyRightType().name(),
            actual.getStudyRightType() == null ? null : actual.getStudyRightType().name());
    }

    private boolean rankMatches(Rank expected, fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RankEntity actual) {
        if(expected == null) {
            return actual == null;
        }
        if(actual == null) {
            return false;
        }
        return expected.getRankAll() == actual.getRankAll()
            && expected.getMaxSeatsAll() == actual.getMaxSeatsAll()
            && expected.getRankCrossStudy() == actual.getRankCrossStudy()
            && expected.getMaxSeatsCrossStudy() == actual.getMaxSeatsCrossStudy();
    }

    private RegistrationReplyRequest getRegistrationReplyTemplate() {
        StudyRightIdentifier createdStudyRightId = new StudyRightIdentifier();
        createdStudyRightId.setStudyRightId("OPISKOIK2");
        createdStudyRightId.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRightStatus createdStudyRightStatus = new StudyRightStatus();
        createdStudyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ACTIVE);
        createdStudyRightStatus.setStartDate(LocalDate.of(2017, 1, 1));
        createdStudyRightStatus.setEndDate(LocalDate.of(2020, 6, 1));

        StudyRight createdStudyRight = new StudyRight();
        createdStudyRight.setIdentifiers(createdStudyRightId);
        createdStudyRight.setStudyRightStatus(createdStudyRightStatus);

        RegistrationReplyRequest replyRequest = new RegistrationReplyRequest();
        replyRequest.setStatus(RegistrationStatus.REGISTERED);
        replyRequest.setHostStudyRight(createdStudyRight);
        replyRequest.setHostStudentEppn("rara@rara.fi");
        replyRequest.setHostStudentNumber("21342143");
        return replyRequest;
    }
}
