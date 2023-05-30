package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Person;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.*;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RegistrationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class StudyRecordRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordRouteV8IntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private ModelMapper modelMapper;

    private int messageSchemaVersion = 8;

    private CourseUnitEntity courseUnitEntity;
    private OrganisationEntity studentHomeOrganization;
    private OrganisationEntity courseUnitOrganizerOrganization;
    private StudyRecordStudent student;
    private RegistrationEntity registration;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ-1", "OJ-1-CODE", "TESTORG2", null, null);
        courseUnitRepository.create(courseUnitEntity);

        studentHomeOrganization = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        courseUnitOrganizerOrganization = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(studentHomeOrganization);
        organisationRepository.create(courseUnitOrganizerOrganization);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.PENDING);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection selectionReply = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED);

        registration = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, null);

        registrationRepository.create(registration);

        StudyRight homeStudyRight = DtoInitializerV8.getStudyRight(studentHomeOrganization.getId());
        StudyRight hostStudyRight = DtoInitializerV8.getStudyRight(courseUnitOrganizerOrganization.getId());

        ExtendedStudent extendedStudent = new ExtendedStudent();
        extendedStudent.setOid("123.456789.12341234");
        extendedStudent.setPersonId("010101-0101");
        extendedStudent.setHomeEppn("mactestington@eppn.fi");
        extendedStudent.setHomeStudentNumber("1234567");
        extendedStudent.setFirstNames("Testi");
        extendedStudent.setSurName("Mac Testington");
        extendedStudent.setGivenName("Testo");
        extendedStudent.setHostStudentNumber("1234566");
        extendedStudent.setHostEppn("testst@testi2.fi");
        extendedStudent.setOid(extendedStudent.getOid());
        extendedStudent.setPersonId(extendedStudent.getPersonId());
        extendedStudent.setHomeStudyRight(homeStudyRight);
        extendedStudent.setHostStudyRight(hostStudyRight);
        
        student = new StudyRecordStudent(extendedStudent);
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_shouldSucceed() throws JMSException, IOException {
        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
                "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
                "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
                "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
                completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
                courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInOrganisation = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier());

        // now the target university replies with a message "yes your study is now recorded"
        StudyRecordReplyRequest replyRequest = new StudyRecordReplyRequest();
        replyRequest.setStatus(StudyRecordStatus.RECORDED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, studentHomeOrganization.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInOrganizingOrganization = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInOrganizingOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), forwardedReply.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), forwardedReply.getStudent().getHostStudyRightIdentifier());
    }

    @Test
    public void testSendingCreateOtherWithoutIdentifiers_shouldSucceed() throws JMSException, IOException {

        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        //Set the study right identifiers as null as we are sending request type OTHER
        student.setHomeStudyRightIdentifier(null);
        student.setHostStudyRightIdentifier(null);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.OTHER);
        //Send the studyrecord request to home school and check that the message was succesfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInOrganisation = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);

        // now the target university replies with a message "yes your study is now recorded"
        StudyRecordReplyRequest replyRequest = new StudyRecordReplyRequest();
        replyRequest.setStatus(StudyRecordStatus.RECORDED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInstudentHomeOrganization = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInstudentHomeOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
    }
    @Test
    public void testSendingCreateOtherWithIdentifiers_shouldSucceed() throws JMSException, IOException {

        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.OTHER);

        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInOrganisation = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier());

        // now the target university replies with a message "yes your study is now recorded"
        StudyRecordReplyRequest replyRequest = new StudyRecordReplyRequest();
        replyRequest.setStatus(StudyRecordStatus.RECORDED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInstudentHomeOrganization = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInstudentHomeOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), forwardedReply.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), forwardedReply.getStudent().getHostStudyRightIdentifier());
    }

    @Test
    public void testSendingCreateOtherWithoutStudentIdOrStudentOid_shouldSucceed() throws JMSException, IOException {

        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.OTHER);

        // send only personId
        createStudyRecordRequest.getStudent().setPersonId("01010101-0101");
        createStudyRecordRequest.getStudent().setOid(null);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // send only oid
        createStudyRecordRequest.getStudent().setPersonId(null);
        createStudyRecordRequest.getStudent().setOid(UUID.randomUUID().toString());

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // send both
        createStudyRecordRequest.getStudent().setPersonId("01010101-0101");
        createStudyRecordRequest.getStudent().setOid(UUID.randomUUID().toString());

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // send neither
        createStudyRecordRequest.getStudent().setPersonId(null);
        createStudyRecordRequest.getStudent().setOid(null);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        JsonValidationFailedResponse failedResponse = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(failedResponse.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateCrossStudyWithoutStudentIdAndStudentOid_shouldSucceed() throws JMSException, IOException {

        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.CROSS_STUDY);

        // dont send personId or oid
        createStudyRecordRequest.getStudent().setPersonId(null);
        createStudyRecordRequest.getStudent().setOid(null);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInStudentHomeOrganisation = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInStudentHomeOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier());

        // now the target university replies with a message "yes your study is now recorded"
        StudyRecordReplyRequest replyRequest = new StudyRecordReplyRequest();
        replyRequest.setStatus(StudyRecordStatus.RECORDED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInCourseUnitOrganizerOrganisation = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInCourseUnitOrganizerOrganisation);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), forwardedReply.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), forwardedReply.getStudent().getHostStudyRightIdentifier());
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_rejectMessageWithReason_shouldSucceed() throws JMSException, IOException {
        Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), Collections.singletonList(completedCredit),
            student, student.getHomeStudyRightIdentifier(), student.getHostStudyRightIdentifier(), RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInOrganisation = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier());

        // now the target university replies with a message "yes your study is now recorded"
        StudyRecordReplyRequest replyRequest = new StudyRecordReplyRequest();
        replyRequest.setStatus(StudyRecordStatus.RECORD_REJECTED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());
        replyRequest.setRejectionReason(new LocalisedString("Hylätty", "Rejected", "Rejekt"));

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, studentHomeOrganization.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus.RECORD_REJECTED, studyRecords.get(0).getStatus());
        assertEquals(replyRequest.getRejectionReason().getValue("fi"), studyRecords.get(0).getRejectionReason().getValue("fi"));
        assertEquals(replyRequest.getRejectionReason().getValue("en"), studyRecords.get(0).getRejectionReason().getValue("en"));
        assertEquals(replyRequest.getRejectionReason().getValue("sv"), studyRecords.get(0).getRejectionReason().getValue("sv"));

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInOrganizingOrganization = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInOrganizingOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(replyRequest.getRejectionReason().getValue("fi"), forwardedReply.getRejectionReason().getValue("fi"));
        assertEquals(replyRequest.getRejectionReason().getValue("en"), forwardedReply.getRejectionReason().getValue("en"));
        assertEquals(replyRequest.getRejectionReason().getValue("sv"), forwardedReply.getRejectionReason().getValue("sv"));
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), forwardedReply.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), forwardedReply.getStudent().getHostStudyRightIdentifier());
    }
}
