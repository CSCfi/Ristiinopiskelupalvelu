package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Person;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Phone;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.acknowledgement.Acknowledgement;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.student.UpdateStudentRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.CreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class AcknowledgementRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AcknowledgementRouteV8IntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private int messageSchemaVersion = 8;

    private NetworkEntity testNetwork;
    private OrganisationEntity sendingOrganisation;
    private OrganisationEntity receivingOrganisation;
    private fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork;
    private CourseUnitEntity courseUnitEntity;

    @BeforeEach
    public void setup() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity validity =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation organisation =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation organisation2 =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        testNetwork = new NetworkEntity();
        testNetwork.setId("CN-1");
        testNetwork.setName(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("verkosto", null, null));
        testNetwork.setOrganisations(Arrays.asList(organisation, organisation2));
        testNetwork.setValidity(validity);
        testNetwork.setPublished(true);
        networkRepository.create(testNetwork);

        sendingOrganisation = new OrganisationEntity();
        sendingOrganisation.setOrganisationName(
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null));
        sendingOrganisation.setId("TESTORG1");
        sendingOrganisation.setQueue("testiorganisaatio1");
        sendingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        receivingOrganisation = new OrganisationEntity();
        receivingOrganisation.setOrganisationName(
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null));
        receivingOrganisation.setId("TESTORG2");
        receivingOrganisation.setQueue("testiorganisaatio2");
        receivingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        cooperationNetwork = DtoInitializer.getCooperationNetwork(
            testNetwork.getId(), testNetwork.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);
    }
    @Test
    public void testSendingAcknowledgementForRegistrationRequest_shouldSucceed() throws JMSException{

        Message responseMessage = sendValidRegistrationRequest();
        final String correlationId = responseMessage.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertNotNull(correlationId);
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        Message receivedRegistrationRequest = jmsTemplate.receive(receivingOrganisation.getQueue());
        String receivedRegisterationRequestCorrelationId =  receivedRegistrationRequest.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, receivedRegisterationRequestCorrelationId);

        //The organisationId should be got from the received message instead.
        Acknowledgement registerationRequestAcknowledgement = new Acknowledgement(sendingOrganisation.getId());
        registerationRequestAcknowledgement.setMessageType(MessageType.CREATE_REGISTRATION_REQUEST);
        registerationRequestAcknowledgement.setRequestId(resp.getRegistrationRequestId());
        Message acknowledgementResponse = JmsHelper.sendAndReceiveAcknowledgement(jmsTemplate, registerationRequestAcknowledgement, receivingOrganisation.getId(), receivedRegisterationRequestCorrelationId);
        String acknowledgementResponseCorrelationId =  acknowledgementResponse.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementResponseCorrelationId);

        Message receivedRegisterationRequestAcknowledgement = jmsTemplate.receive(sendingOrganisation.getQueue());
        String acknowledgementCorrelationId = receivedRegisterationRequestAcknowledgement.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementCorrelationId);

    }
    @Test
    public void testSendingAcknowledgementForUpdateStudentRequest_shouldSucceed() throws JMSException {

        Message responseMessage = sendValidStudentUpdateRequest();
        final String correlationId = responseMessage.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertNotNull(correlationId);

        Message receivedStudendUpdateMessage = jmsTemplate.receive(receivingOrganisation.getQueue());
        String receivedStudendUpdateMessageCorrelationId = receivedStudendUpdateMessage.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, receivedStudendUpdateMessageCorrelationId);

        //The organisationId should be got from the received message instead.
        Acknowledgement studentUpdateAcknowledgement = new Acknowledgement(sendingOrganisation.getId());
        studentUpdateAcknowledgement.setMessageType(MessageType.UPDATE_STUDENT_REQUEST);
        Message acknowledgementResponse = JmsHelper.sendAndReceiveAcknowledgement(jmsTemplate, studentUpdateAcknowledgement, receivingOrganisation.getId(), receivedStudendUpdateMessageCorrelationId);
        String acknowledgementResponseCorrelationId =  acknowledgementResponse.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementResponseCorrelationId);

        Message receivedStudentUpdateAcknowledgement = jmsTemplate.receive(sendingOrganisation.getQueue());
        String acknowledgementCorrelationId = receivedStudentUpdateAcknowledgement.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementCorrelationId);
    }

    @Test
    public void testSendingAcknowledgementForCreateStudyRecordRequest_shouldSucceed() throws JMSException {

        Message responseMessage = sendValidCreateStudyRecordRequest();
        final String correlationId = responseMessage.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertNotNull(correlationId);

        Message receivedStudyRecordMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        String receivedStudyRecordMessageCorrelationId = receivedStudyRecordMessage.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, receivedStudyRecordMessageCorrelationId);

        //The organisationId should be got from the received message instead.
        Acknowledgement studyRecordAcknowledgement = new Acknowledgement(receivingOrganisation.getId());
        studyRecordAcknowledgement.setMessageType(MessageType.CREATE_STUDYRECORD_REQUEST);
        Message acknowledgementResponse = JmsHelper.sendAndReceiveAcknowledgement(jmsTemplate, studyRecordAcknowledgement, receivingOrganisation.getId(), receivedStudyRecordMessageCorrelationId);
        String acknowledgementResponseCorrelationId =  acknowledgementResponse.getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementResponseCorrelationId);

        Message receivedStudyRecordAcknowledgement = jmsTemplate.receive(receivingOrganisation.getQueue());
        String acknowledgementCorrelationId = receivedStudyRecordAcknowledgement .getStringProperty(MessageHeader.RIPA_CORRELATION_ID);
        assertEquals(correlationId, acknowledgementCorrelationId);
    }

    private Message sendValidRegistrationRequest() {

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        return JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
    }

    private Message sendValidStudentUpdateRequest () {

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);
        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(),
            receivingOrganisation.getId(), "CN-1");
        req.setSelections(Collections.singletonList(selection));
        JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        Message receivedCreateRegistrationMessage = jmsTemplate.receive(receivingOrganisation.getQueue());
        // now send update student info request
        UpdateStudentRequest updateRequest = new UpdateStudentRequest();
        updateRequest.setPersonId(req.getStudent().getPersonId());
        updateRequest.setOid(req.getStudent().getOid());
        updateRequest.setFirstNames("Teppo");
        updateRequest.setSurName("Testaaja");
        updateRequest.setCountryOfCitizenship(Collections.singletonList(Country.FI));

        Phone phone = new Phone();
        phone.setNumber("0401111111");
        phone.setDescription("new number");
        updateRequest.setPhone(Collections.singletonList(phone));

        return  JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
    }
    private Message sendValidCreateStudyRecordRequest() {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection selection =
            DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(),
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.PENDING);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection selectionReply =
            DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(),
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED);

        RegistrationEntity registration = EntityInitializer.getRegistrationEntity(
            sendingOrganisation.getId(), receivingOrganisation.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED,
            null);

        registrationRepository.create(registration);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight homeStudyRight = DtoInitializerV8.getStudyRight(sendingOrganisation.getId());
        fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight hostStudyRight = DtoInitializerV8.getStudyRight(receivingOrganisation.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent registrationStudentV8 = new fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent();
        registrationStudentV8.setOid(registration.getStudent().getOid());
        registrationStudentV8.setPersonId(registration.getStudent().getPersonId());
        registrationStudentV8.setHomeEppn("mactestington@eppn.fi");
        registrationStudentV8.setHomeStudentNumber("1234567");
        registrationStudentV8.setFirstNames("Testi");
        registrationStudentV8.setSurName("Mac Testington");
        registrationStudentV8.setGivenName("Testo");
        registrationStudentV8.setHostStudentNumber("1234566");
        registrationStudentV8.setHostEppn("testst@testi2.fi");
        registrationStudentV8.setHomeStudyRight(homeStudyRight);
        registrationStudentV8.setHostStudyRight(hostStudyRight);

        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456",
            new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            receivingOrganisation.getId(), sendingOrganisation.getId(), Collections.singletonList(completedCredit), registrationStudentV8,
            homeStudyRight.getIdentifiers(), hostStudyRight.getIdentifiers(), RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        return JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, receivingOrganisation.getId());

    }
}
