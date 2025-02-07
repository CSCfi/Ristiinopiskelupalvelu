package fi.uta.ristiinopiskelu.handler.integration.route.current;

import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.handler.processor.PersonIdValidatorProcessor;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.*;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
import fi.uta.ristiinopiskelu.persistence.repository.*;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class StudyRecordRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private PersonIdValidatorProcessor personIdValidatorProcessor;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    private NetworkEntity networkEntity;
    private RealisationEntity realisationEntity;
    private CourseUnitEntity courseUnitEntity;
    private OrganisationEntity studentHomeOrganization;
    private OrganisationEntity courseUnitOrganizerOrganization;
    private StudyRecordStudent student;
    private RegistrationEntity registration;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        studentHomeOrganization = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        courseUnitOrganizerOrganization = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(studentHomeOrganization);
        organisationRepository.create(courseUnitOrganizerOrganization);

        NetworkOrganisation networkOrganisation1 = new NetworkOrganisation();
        networkOrganisation1.setOrganisationTkCode(studentHomeOrganization.getId());
        networkOrganisation1.setValidityInNetwork(DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1)));
        networkOrganisation1.setIsCoordinator(true);

        NetworkOrganisation networkOrganisation2 = new NetworkOrganisation();
        networkOrganisation2.setOrganisationTkCode(courseUnitOrganizerOrganization.getId());
        networkOrganisation2.setValidityInNetwork(DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1)));
        networkOrganisation2.setIsCoordinator(true);

        networkEntity = EntityInitializer.getNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(networkOrganisation1, networkOrganisation2),
            DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);
        networkRepository.create(networkEntity);

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, networkEntity.getValidity().getStart().toLocalDate(), null);

        courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ-1", "OJ-1-CODE", "TESTORG2", Collections.singletonList(network), null);
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference courseUnitReference = new StudyElementReference(courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);

        realisationEntity = EntityInitializer.getRealisationEntity(
            "TOT-1", "TOT1-CODE", "TESTORG2", Collections.singletonList(courseUnitReference), Collections.singletonList(network));
        realisationRepository.create(realisationEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection rootSelection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, selection, null);

        RegistrationSelection selectionReply = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.ACCEPTED);

        RegistrationSelection rootSelectionReply = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ACCEPTED, selectionReply, null);

        registration = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(rootSelection),
            Collections.singletonList(rootSelectionReply), RegistrationStatus.REGISTERED, networkEntity.getId());

        registrationRepository.create(registration);
        student = new StudyRecordStudent(registration.getStudent());
    }

    @Test
    public void testSendingCreateStudyRecordMessage_registrationsCheckedAccordingToEnrolmentDateTimeAndReceivingDateTime_shouldSucceed() throws JMSException {
        // clear all registrations premade in setUp()
        registrationRepository.deleteAll();

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection selectionReply = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.ACCEPTED);

        // create a few registrations with the same enrolmentDateTime, with or without receivingDateTime
        RegistrationEntity registration = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), RegistrationStatus.REGISTERED, networkEntity.getId());
        registration.setEnrolmentDateTime(OffsetDateTime.now().minusDays(5));
        registration.setReceivingDateTime(null);

        RegistrationEntity registration2 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), RegistrationStatus.REGISTRATION_REJECTED, networkEntity.getId());
        registration2.setStudent(registration.getStudent());
        registration2.setEnrolmentDateTime(OffsetDateTime.now().minusDays(5));
        registration2.setReceivingDateTime(null);

        RegistrationEntity registration3 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), RegistrationStatus.REGISTERED, networkEntity.getId());
        registration3.setStudent(registration.getStudent());
        registration3.setEnrolmentDateTime(OffsetDateTime.now().minusDays(5));
        registration3.setReceivingDateTime(OffsetDateTime.now().minusDays(1));

        registration = registrationRepository.create(registration);
        registrationRepository.create(registration2);
        registrationRepository.create(registration3);

        // create the study record request
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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), new StudyRecordStudent(registration.getStudent()), RoutingType.CROSS_STUDY);

        // now send the study record request to home school and check that it succeeds
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());

        // now add a new registration with the same enrolmentDateTime as the first registration, but with a recent receivingDateTime and status rejected
        RegistrationEntity registration4 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Collections.singletonList(selection),
            Collections.singletonList(selectionReply), RegistrationStatus.REGISTRATION_REJECTED, networkEntity.getId());
        registration4.setStudent(registration.getStudent());
        registration4.setEnrolmentDateTime(registration.getEnrolmentDateTime());
        registration4.setReceivingDateTime(OffsetDateTime.now());

        registrationRepository.create(registration4);

        // send the study record request to home school and check that it now fails
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.FAILED, resp.getStatus());
    }

    @Test
    public void testSendingCreateStudyRecordMessageForRootSelectionRealisation_shouldSucceed() throws JMSException, IOException {
        Person acceptor = DtoInitializer.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializer.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializer.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        CompletedCreditTarget completedCreditTarget = DtoInitializer.getCompletedCreditTarget(
            realisationEntity.getRealisationId(), realisationEntity.getRealisationIdentifierCode(), CompletedCreditTargetType.REALISATION);

        CompletedCredit completedCredit = DtoInitializer.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            completedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

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
        assertEquals(receivedRequest.getCompletedCredits().get(0).getCompletedCreditTarget().getCompletedCreditTargetId(), createStudyRecordRequest.getCompletedCredits().get(0).getCompletedCreditTarget().getCompletedCreditTargetId());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier());
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInOrganizingOrganization = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInOrganizingOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus(), forwardedReply.getStatus());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getCompletedCredits().get(0).getCompletedCreditTarget().getCompletedCreditTargetId(), forwardedReply.getCompletedCredits().get(0).getCompletedCreditTarget().getCompletedCreditTargetId());
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier(), forwardedReply.getStudent().getHomeStudyRightIdentifier());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier(), forwardedReply.getStudent().getHostStudyRightIdentifier());
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_shouldSucceed() throws JMSException, IOException {
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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

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
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

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

    // disable this test for now. for some unknown reason, overriding onlyTestPersonsAllowed to true seems to be impossible with *any* method.
    // this certainly isn't normal behaviour, simple @TestPropertySource or @DynamicPropertySource should absolutely suffice, not to mention setting the value
    // through listeners etc. computer just says no.
    @Disabled
    @DirtiesContext
    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_withOnlyTestPersonsAllowed_shouldFail() throws JMSException {
        // disallow anything but test users
        personIdValidatorProcessor.setOnlyTestPersonsAllowed(true);

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

        // test with only person id first
        student.setOid(null);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        // send the study record request to home school and check that it failed.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.FAILED, resp.getStatus());

        // now, test with oid only
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        student.setPersonId(null);

        createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        // send it again, should fail too
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.FAILED, resp.getStatus());
    }

    @Test
    public void testSendingCreateOtherWithoutIdentifiers_shouldSucceed() throws JMSException, IOException {

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

        //Set the study right identifiers as null as we are sending request type OTHER
        student.setHomeStudyRightIdentifier(null);
        student.setHostStudyRightIdentifier(null);

        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.OTHER);
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
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

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
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.OTHER);

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
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

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
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.OTHER);

        // send only personId
        createStudyRecordRequest.getStudent().setPersonId(HetuUtil.generateRandom());
        createStudyRecordRequest.getStudent().setOid(null);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // send only oid
        createStudyRecordRequest.getStudent().setPersonId(null);
        createStudyRecordRequest.getStudent().setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, studentHomeOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // send both
        createStudyRecordRequest.getStudent().setPersonId(HetuUtil.generateRandom());
        createStudyRecordRequest.getStudent().setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

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
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

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
        assertEquals(receivedRequest.getNetworkIdentifier(), networkEntity.getId());

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
        assertEquals(StudyRecordStatus.RECORD_REJECTED, studyRecords.get(0).getStatus());
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

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_withMultipleRegistrationsAndonlyLatestRegistrationShouldCount_shouldSucceed() throws JMSException {

        // first, add another registration with REJECTED status in addition to the one in setUp()
        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationEntity registration2 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(),
            Collections.singletonList(selection),
            Collections.singletonList(DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.REJECTED)),
            RegistrationStatus.REGISTERED, networkEntity.getId());

        registration2.getStudent().setOid(student.getOid());
        registration2.getStudent().setPersonId(student.getPersonId());

        registrationRepository.create(registration2);

        assertEquals(2, registrationRepository.findAll(Pageable.unpaged()).getTotalElements());

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

        // try creating a study record for the first time. should fail now since the latest is "REJECTED"
        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse failedResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(failedResp.getStatus() == Status.FAILED);

        // now add some more registrations. the latest has succeeded.
        RegistrationEntity registration3 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(),
            Collections.singletonList(selection),
            Collections.singletonList(DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.REJECTED)),
            RegistrationStatus.REGISTERED, networkEntity.getId());

        registration3.getStudent().setOid(student.getOid());
        registration3.getStudent().setPersonId(student.getPersonId());

        registrationRepository.create(registration3);

        RegistrationEntity registration4 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(),
            Collections.singletonList(selection),
            Collections.singletonList(DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.ACCEPTED)),
            RegistrationStatus.REGISTERED, networkEntity.getId());

        registration4.getStudent().setOid(student.getOid());
        registration4.getStudent().setPersonId(student.getPersonId());

        registrationRepository.create(registration4);

        assertEquals(4, registrationRepository.findAll(Pageable.unpaged()).getTotalElements());

        // now try again, should work
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // and finally, add one more REJECTED registration and try again. should fail again.
        RegistrationEntity registration5 = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(),
            Collections.singletonList(selection),
            Collections.singletonList(DtoInitializer.getRegistrationSelectionCourseUnit(courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.REJECTED)),
            RegistrationStatus.REGISTERED, networkEntity.getId());

        registration5.getStudent().setOid(student.getOid());
        registration5.getStudent().setPersonId(student.getPersonId());

        registrationRepository.create(registration5);

        assertEquals(5, registrationRepository.findAll(Pageable.unpaged()).getTotalElements());

        // should fail
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        failedResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(failedResp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_withRegistrationsToSameNameButDifferentTypeElements_shouldSucceed() throws JMSException {
        // delete all existing registrations first
        registrationRepository.deleteAll();

        // now recreate a single registration with two selections that both have the same completedCreditTargetId (courseUnitEntity.getStudyElementId() for the realisation too)
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(courseUnitEntity.getStudyElementId(), courseUnitOrganizerOrganization.getId(), Collections.emptyList(),
            Collections.singletonList(DtoInitializer.getCooperationNetwork(networkEntity.getId(), null, true, networkEntity.getValidity().getStart().toLocalDate(), null)));
        realisationRepository.create(realisationEntity);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        RegistrationSelection realisationSelectionReply = DtoInitializer.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ACCEPTED, null, null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection courseUnitSelectionReply = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.ACCEPTED);

        RegistrationEntity registration = EntityInitializer.getRegistrationEntity(
            studentHomeOrganization.getId(), courseUnitOrganizerOrganization.getId(), Arrays.asList(realisationSelection, courseUnitSelection),
            Arrays.asList(realisationSelectionReply, courseUnitSelectionReply), RegistrationStatus.REGISTERED, networkEntity.getId());

        registration.getStudent().setPersonId(student.getPersonId());
        registration.getStudent().setOid(student.getOid());

        registrationRepository.create(registration);

        assertEquals(1, registrationRepository.findAll(Pageable.unpaged()).getTotalElements());

        // now the completedCredit
        Person acceptor = DtoInitializer.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        CompletedCreditAssessment completedCreditAssessment = DtoInitializer.getCompletedCreditAssessment(
            "completed credit assessment description", "5", ScaleValue.FIVE_LEVEL, GradeCode.GRADE_5);

        StudyRecordOrganisation organisation = DtoInitializer.getStudyRecordOrganisation(
            "ORG-2", "123456", new LocalisedString("Nimen kuvaus", null, null));

        // completed credit for the COURSE_UNIT
        CompletedCreditTarget courseUnitCompletedCreditTarget = DtoInitializer.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.COURSE_UNIT);

        CompletedCredit courseUnitCompletedCredit = DtoInitializer.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            courseUnitCompletedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        // completed credit for the REALISATION
        CompletedCreditTarget realisationCompletedCreditTarget = DtoInitializer.getCompletedCreditTarget(
            realisationEntity.getRealisationId(), realisationEntity.getRealisationIdentifierCode(), CompletedCreditTargetType.REALISATION);

        CompletedCredit realisationCompletedCredit = DtoInitializer.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            realisationCompletedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        // send a study record for the COURSE_UNIT
        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(courseUnitCompletedCredit), student, RoutingType.CROSS_STUDY);

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // send a study record for the REALISATION
        createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(realisationCompletedCredit), student, RoutingType.CROSS_STUDY);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // send a study record for both COURSE_UNIT and REALISATION at the same time
        createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Arrays.asList(realisationCompletedCredit, courseUnitCompletedCredit), student, RoutingType.CROSS_STUDY);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // send a study record for STUDY_MODULE with the same studyElementId; this should not work
        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(courseUnitEntity.getStudyElementId(),
            courseUnitEntity.getStudyElementIdentifierCode(), courseUnitEntity.getOrganizingOrganisationId(),
            Collections.singletonList(DtoInitializer.getCooperationNetwork(networkEntity.getId(), null, true, networkEntity.getValidity().getStart().toLocalDate(), null)),
            new LocalisedString("test", null, null));
        studyModuleRepository.create(studyModuleEntity);

        CompletedCreditTarget studyModuleCompletedCreditTarget = DtoInitializer.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), CompletedCreditTargetType.STUDY_MODULE);

        CompletedCredit studyModuleCompletedCredit = DtoInitializer.getCompletedCreditForCourseUnit(
            "SUORITUSID-1", new LocalisedString("testisuoritus", null, null), CompletedCreditStatus.ACCEPTED,
            studyModuleCompletedCreditTarget, CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor), completedCreditAssessment, "ORG-2", organisation);

        createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(studyModuleCompletedCredit), student, RoutingType.CROSS_STUDY);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse failedResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(failedResp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingCreateStudyRecordMessageV9toV8_shouldSucceed() throws JMSException, IOException {

        studentHomeOrganization = EntityInitializer.getOrganisationEntity("TESTORG1", "testiorganisaatio1",
            new LocalisedString("Opiskelijan kotiorganisaatio testiorganisaatio", null, null), this.messageSchemaVersion - 1);

        organisationRepository.create(studentHomeOrganization);

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
            courseUnitOrganizerOrganization.getId(),
            studentHomeOrganization.getId(),
            networkEntity.getId(),
            Collections.singletonList(completedCredit),
            student,
            RoutingType.CROSS_STUDY);
        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        //The response is in schema version 6
        Message messageReceivedInOrganisation = jmsTemplate.receive(studentHomeOrganization.getQueue());
        fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.ForwardedCreateStudyRecordRequest receivedRequest = (fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference());

        // now the target university replies with a message "yes your study is now recorded"
        //The reply is in schema version 6.
        fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.StudyRecordReplyRequest replyRequest = new fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.StudyRecordReplyRequest();
        replyRequest.setStatus(fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStatus.RECORDED);
        replyRequest.setStudyRecordRequestId(receivedRequest.getStudyRecordRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, studentHomeOrganization.getId());
        fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse replyResponse = (fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);

        // now we should have one study record with RECORDED status in the repository
        List<StudyRecordEntity> studyRecords = StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studyRecords.size());
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        Message replyReceivedInstudentHomeOrganization = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        ForwardedStudyRecordReplyRequest forwardedReply = (ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInstudentHomeOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus().name(), forwardedReply.getStatus().name());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        //assertEquals(receivedRequest.getCompletedCredits().get(0), forwardedReply.getCompletedCredits().get(0));
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference(), forwardedReply.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getStudyRightId(), forwardedReply.getStudent().getHostStudyRightIdentifier().getStudyRightId());
    }

    @Test
    public void testSendingCreateStudyRecordMessageV8toV9_shouldSucceed() throws JMSException, IOException {

        courseUnitOrganizerOrganization = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion - 1);
        organisationRepository.create(courseUnitOrganizerOrganization);

        //All sent DTO:s are in v7 format
        fi.uta.ristiinopiskelu.datamodel.dto.v8.Person acceptor = DtoInitializerV8.getPerson("Olli", "Opettaja", "testvir@testi.fi", "testivir@testi2.fi");

        fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditAssessment completedCreditAssessment = DtoInitializerV8.getCompletedCreditAssessment(
            "completed credit assessment description", "5", fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.ScaleValue.FIVE_LEVEL, fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.GradeCode.GRADE_5);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordOrganisation organisation = DtoInitializerV8.getStudyRecordOrganisation(
            "ORG-2", "123456", new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("Nimen kuvaus", null, null));

        fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditTarget completedCreditTarget = DtoInitializerV8.getCompletedCreditTarget(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getStudyElementIdentifierCode(), fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditTargetType.COURSE_UNIT);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCredit completedCredit = DtoInitializerV8.getCompletedCreditForCourseUnit(
            "SUORITUSID-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString("testisuoritus", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditStatus.ACCEPTED,
            completedCreditTarget,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCreditType.DEGREE_PROGRAMME_COMPLETION,
            Collections.singletonList(acceptor),
            completedCreditAssessment,
            "ORG-2",
            organisation);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier homeStudyRightIdentifier = new fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier();
        homeStudyRightIdentifier.setStudyRightId(registration.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        homeStudyRightIdentifier.setOrganisationTkCodeReference(registration.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier hostStudyRightIdentifier = new fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier();
        hostStudyRightIdentifier.setStudyRightId(registration.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        hostStudyRightIdentifier.setOrganisationTkCodeReference(registration.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializerV8.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(),
            studentHomeOrganization.getId(),
            Collections.singletonList(completedCredit),
            registration.getStudent(),
            homeStudyRightIdentifier,
            hostStudyRightIdentifier,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.StudyRecordResponse resp = (fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        //Read the forwarded request as the student's home school and check that data matches the sent request.
        Message messageReceivedInOrganisation = jmsTemplate.receive(studentHomeOrganization.getQueue());
        ForwardedCreateStudyRecordRequest receivedRequest = (ForwardedCreateStudyRecordRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);
        assertNotNull(receivedRequest);
        assertEquals(resp.getStudyRecordRequestId(), receivedRequest.getStudyRecordRequestId());
        assertEquals(receivedRequest.getCompletedCredits().get(0).getMinEduGuidanceArea(), MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getStudyRightId(), createStudyRecordRequest.getStudent().getHostStudyRightIdentifier().getStudyRightId());

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
        assertEquals(StudyRecordStatus.RECORDED, studyRecords.get(0).getStatus());

        //Read the forwarded study record reply as the course unit's organizing school and check the data
        // Again, response is in v7 format
        Message replyReceivedInstudentHomeOrganization = jmsTemplate.receive(courseUnitOrganizerOrganization.getQueue());
        fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.ForwardedStudyRecordReplyRequest forwardedReply = (fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.ForwardedStudyRecordReplyRequest) jmsTemplate.getMessageConverter().fromMessage(replyReceivedInstudentHomeOrganization);
        assertNotNull(forwardedReply);
        assertEquals(resp.getStudyRecordRequestId(), forwardedReply.getStudyRecordRequestId());
        assertEquals(replyRequest.getStatus().toString(), forwardedReply.getStatus().toString());
        assertEquals(forwardedReply.getCompletedCredits().get(0).getMinEduGuidanceArea(), fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.MinEduGuidanceArea.EDUCATION);
        assertEquals(receivedRequest.getStudent().getFirstNames(), forwardedReply.getStudent().getFirstNames());
        assertEquals(receivedRequest.getStudent().getHomeEppn(), forwardedReply.getStudent().getHomeEppn());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getStudyRightId(), forwardedReply.getStudent().getHostStudyRightIdentifier().getStudyRightId());
        assertEquals(receivedRequest.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference(), forwardedReply.getStudent().getHostStudyRightIdentifier().getOrganisationTkCodeReference());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier().getStudyRightId(), forwardedReply.getStudent().getHomeStudyRightIdentifier().getStudyRightId());
        assertEquals(receivedRequest.getStudent().getHomeStudyRightIdentifier().getOrganisationTkCodeReference(), forwardedReply.getStudent().getHomeStudyRightIdentifier().getOrganisationTkCodeReference());
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnit_onlyStudentOidOrPersonIdShouldSuffice_shouldSucceed() throws JMSException, IOException {
        // first, remove the existing person's personId in the registration so that only oid remains
        RegistrationEntity existingRegCreatedInSetUp = registrationRepository.findById(registration.getId()).get();
        existingRegCreatedInSetUp.getStudent().setPersonId(null);
        registrationRepository.update(existingRegCreatedInSetUp);

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

        // now, we send a study record request with student info that has BOTH personId and oid defined. this failed previously.
        CreateStudyRecordRequest createStudyRecordRequest = MessageTemplateInitializer.getCreateStudyRecordRequestTemplate(
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        //Send the study record request to home school and check that the message was successfully forwarded.
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));

        // then, test the same so that only personId remains
        existingRegCreatedInSetUp = registrationRepository.findById(registration.getId()).get();
        existingRegCreatedInSetUp.getStudent().setPersonId(student.getPersonId());
        existingRegCreatedInSetUp.getStudent().setOid(null);
        registrationRepository.update(existingRegCreatedInSetUp);

        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnitWithNotYetValidNetwork_shouldFail() throws JMSException {
        networkEntity.setValidity(new Validity(Validity.ContinuityEnum.INDEFINITELY, OffsetDateTime.now().plusDays(2), null));
        networkEntity = networkRepository.update(networkEntity);

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().plusDays(2), null);

        courseUnitEntity.setCooperationNetworks(Collections.singletonList(network));
        courseUnitEntity = courseUnitRepository.update(courseUnitEntity);

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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        // Send the study record request to home school and check that it failed
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.FAILED, resp.getStatus());
    }

    @Test
    public void testSendingCreateStudyRecordMessageForCourseUnitWithExpiredNetwork_shouldSucceed() throws JMSException {
        networkEntity.setValidity(new Validity(Validity.ContinuityEnum.INDEFINITELY, OffsetDateTime.now().minusDays(5), OffsetDateTime.now().minusDays(2)));
        networkEntity = networkRepository.update(networkEntity);

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), null, true, LocalDate.now().minusDays(5), LocalDate.now().minusDays(2));

        courseUnitEntity.setCooperationNetworks(Collections.singletonList(network));
        courseUnitEntity = courseUnitRepository.update(courseUnitEntity);

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
            courseUnitOrganizerOrganization.getId(), studentHomeOrganization.getId(), networkEntity.getId(),
            Collections.singletonList(completedCredit), student, RoutingType.CROSS_STUDY);

        // Send the study record request to home school and check that it succeeded
        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyRecordRequest, courseUnitOrganizerOrganization.getId());
        StudyRecordResponse resp = (StudyRecordResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertEquals(Status.OK, resp.getStatus());
        assertTrue(StringUtils.isNotBlank(resp.getStudyRecordRequestId()));
    }
}
