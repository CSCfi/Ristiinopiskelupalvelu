package fi.uta.ristiinopiskelu.handler.integration.route.current;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.helper.MessageTemplateInitializer;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.CreateStudyRecordRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.JMSException;
import javax.jms.Message;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class AcknowledgementRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AcknowledgementRouteIntegrationTest.class);

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

    @Value("${general.messageSchema.version}")
    private int messageSchemaVersion;

    private NetworkEntity testNetwork;
    private OrganisationEntity sendingOrganisation;
    private OrganisationEntity receivingOrganisation;
    private CooperationNetwork cooperationNetwork;
    private CourseUnitEntity courseUnitEntity;

    @BeforeEach
    public void setup() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        testNetwork = new NetworkEntity();
        testNetwork.setId("CN-1");
        testNetwork.setName(new LocalisedString("verkosto", null, null));
        testNetwork.setOrganisations(Arrays.asList(organisation, organisation2));
        testNetwork.setValidity(validity);
        testNetwork.setPublished(true);
        networkRepository.create(testNetwork);

        sendingOrganisation = new OrganisationEntity();
        sendingOrganisation.setOrganisationName(new LocalisedString("Lähettävä testiorganisaatio", null, null));
        sendingOrganisation.setId("TESTORG1");
        sendingOrganisation.setQueue("testiorganisaatio1");
        sendingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        receivingOrganisation = new OrganisationEntity();
        receivingOrganisation.setOrganisationName(new LocalisedString("Vastaanottava testiorganisaatio", null, null));
        receivingOrganisation.setId("TESTORG2");
        receivingOrganisation.setQueue("testiorganisaatio2");
        receivingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        cooperationNetwork = DtoInitializer.getCooperationNetwork(
            testNetwork.getId(), testNetwork.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
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

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        return JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
    }

    private Message sendValidStudentUpdateRequest () {

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);
        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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
        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection selectionReply = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.ACCEPTED);

        RegistrationEntity registration;
        registration = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            Collections.singletonList(selection), Collections.singletonList(selectionReply), RegistrationStatus.REGISTERED,
            testNetwork.getId());

        registrationRepository.create(registration);
        StudyRecordStudent student = new StudyRecordStudent(registration.getStudent());
        Person acceptor = DtoInitializer.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializer.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializer.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializer.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializer.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            receivingOrganisation.getId(), sendingOrganisation.getId(), testNetwork.getId(), Collections.singletonList(completedCredit),
            student, RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        return JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, receivingOrganisation.getId());

    }
}
