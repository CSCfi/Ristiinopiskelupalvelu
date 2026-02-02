package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightStatusValue;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.StudyRightType;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.ForwardedCreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.RegistrationReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.student.*;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
import fi.uta.ristiinopiskelu.messaging.util.OidPersonNode;
import fi.uta.ristiinopiskelu.persistence.repository.*;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

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
public class StudentRouteV8IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(StudentRouteV8IntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ModelMapper modelMapper;

    private int messageSchemaVersion = 8;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_shouldSucceed() throws JMSException {
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

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
                "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("verkosto", null, null),
            Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG1", "testiorganisaatio1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG2", "testiorganisaatio2",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            replySelections.add(sel);
        }

        RegistrationReplyRequest replyRequest = new RegistrationReplyRequest();
        replyRequest.setSelections(replySelections);
        replyRequest.setStatus(RegistrationStatus.REGISTERED);
        replyRequest.setHostStudyRight(req.getStudent().getHomeStudyRight());
        replyRequest.setHostStudentEppn("rara@rara.fi");
        replyRequest.setHostStudentNumber("21342143");
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() ==
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        // Consume message from sending organisation queue (forwarded by sending registration status message)
        Message forwardedRegistrationStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(req.getStudent().getPersonId());
        updateStudentStudyRightRequest.setOid(req.getStudent().getOid());

        StudentStudyRight studentStudyRight = modelMapper.map(req.getStudent().getHomeStudyRight(), StudentStudyRight.class);
        studentStudyRight.setEligibleForNetworkStudies(false);
        updateStudentStudyRightRequest.setHomeStudyRight(studentStudyRight);

        Message cancelResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentStudyRightRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(cancelResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightRequest receivedUpdateRequest =
            (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        assertEquals(cancelResp.getStudentRequestId(), receivedUpdateRequest.getStudentRequestId());
        assertEquals(updateStudentStudyRightRequest.getOid(), receivedUpdateRequest.getOid());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            receivedUpdateRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getStudyRightId(),
            receivedUpdateRequest.getHomeStudyRight().getIdentifiers().getStudyRightId());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getStartDate(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getStartDate());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getEndDate(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getEndDate());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightType(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightType());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getEligibleForNetworkStudies(),
            receivedUpdateRequest.getHomeStudyRight().getEligibleForNetworkStudies());

        assertEquals(1, receivedUpdateRequest.getHomeStudyRight().getKeywords().size());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getKey(),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getKey());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getKeySet(),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getKeySet());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("fi"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("fi"));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("en"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("en"));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("sv"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("sv"));

        // Test host study right set
        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertEquals(reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId(),
            receivedUpdateRequest.getHostStudyRightIdentifiers().get(0).getStudyRightId());
        assertEquals(reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            receivedUpdateRequest.getHostStudyRightIdentifiers().get(0).getOrganisationTkCodeReference());

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());
        assertTrue(StringUtils.isNotBlank(cancelResp.getStudentRequestId()));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            studentEntities.get(0).getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getStudyRightId(),
            studentEntities.get(0).getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightType(),
            studentEntities.get(0).getHomeStudyRight().getStudyRightType());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getEligibleForNetworkStudies(),
            studentEntities.get(0).getHomeStudyRight().getEligibleForNetworkStudies());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType.UPDATE_STUDENT_STUDYRIGHT, studentEntities.get(0).getMessageType());
        assertEquals(sendingOrganisation.getId(), studentEntities.get(0).getHomeOrganisationTkCode());
        assertNotNull(studentEntities.get(0).getTimestamp());

        // Target organisation replies to message that they have received it
        StudentReplyRequest cancelReplyMessage = new UpdateStudentStudyRightReplyRequest();
        cancelReplyMessage.setStudentRequestId(receivedUpdateRequest.getStudentRequestId());
        cancelReplyMessage.setStatus(StudentStatus.UPDATED);

        Message cancelStatusResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, cancelReplyMessage, receivingOrganisation.getId());
        DefaultResponse cancelStatusResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(cancelStatusResponseMessage);
        assertTrue(cancelStatusResp.getStatus() == Status.OK);

        studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        Message forwardedCancelStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightReplyRequest receivedForwardedCancelStatusMessage =
            (ForwardedUpdateStudentStudyRightReplyRequest) jmsTemplate.getMessageConverter().fromMessage(forwardedCancelStatusMessage);
        assertEquals(cancelReplyMessage.getStudentRequestId(), receivedForwardedCancelStatusMessage.getStudentRequestId());
        assertEquals(cancelReplyMessage.getStatus(), receivedForwardedCancelStatusMessage.getStatus());
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_testOnlyHostStudyRightsForwardedCorrectly_shouldSucceed() throws JMSException {
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation2 = EntityInitializer.getOrganisationEntity(
            "TESTORG3", "testiorganisaatio3",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);
        organisationRepository.create(receivingOrganisation2);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier homeStudyRightIdentifiers =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier();
        homeStudyRightIdentifiers.setStudyRightId("OIK1");
        homeStudyRightIdentifiers.setOrganisationTkCodeReference(sendingOrganisation.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight studyRightToUpdate =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight();
        studyRightToUpdate.setIdentifiers(homeStudyRightIdentifiers);

        // Create multiple host study rights for host
        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, null);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier hostStudyRightIdentifiers =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier();
        hostStudyRightIdentifiers.setStudyRightId("OIK2");
        hostStudyRightIdentifiers.setOrganisationTkCodeReference(receivingOrganisation.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight hostStudyRight =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight();
        hostStudyRight.setIdentifiers(hostStudyRightIdentifiers);

        RegistrationStudentEntity student = RegistrationStudentEntity.builder()
            .homeStudyRight(StudyRightEntity.fromStudyRight(studyRightToUpdate))
            .hostStudyRight(StudyRightEntity.fromStudyRight(hostStudyRight))
            .build();

        registrationEntity.setStudent(student);
        registrationRepository.create(registrationEntity);

        RegistrationEntity registrationEntity2 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, null);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier hostStudyRightIdentifiers2 =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier();
        hostStudyRightIdentifiers2.setStudyRightId("OIK3");
        hostStudyRightIdentifiers2.setOrganisationTkCodeReference(receivingOrganisation.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight hostStudyRight2 =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight();
        hostStudyRight2.setIdentifiers(hostStudyRightIdentifiers2);

        RegistrationStudentEntity student2 = RegistrationStudentEntity.builder()
            .homeStudyRight(StudyRightEntity.fromStudyRight(studyRightToUpdate))
            .hostStudyRight(StudyRightEntity.fromStudyRight(hostStudyRight2))
            .build();

        registrationEntity2.setStudent(student2);
        registrationRepository.create(registrationEntity2);

        // Create one registration with same study right identifiers as first (OIK2) (should not be duplicated)
        RegistrationEntity registrationEntityWithSameStudyRightsAs1 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTRATION_REJECTED, null);

        registrationEntityWithSameStudyRightsAs1.setStudent(student);
        registrationRepository.create(registrationEntityWithSameStudyRightsAs1);

        // Create one registration for receivingOrganisation 2
        RegistrationEntity registrationEntityForOtherOrg = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation2.getId(),
            null, fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, null);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier hostStudyRightIdentifiers3 =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier();
        hostStudyRightIdentifiers3.setStudyRightId("OIK4");
        hostStudyRightIdentifiers3.setOrganisationTkCodeReference(receivingOrganisation2.getId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight hostStudyRight3 = new fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight();
        hostStudyRight3.setIdentifiers(hostStudyRightIdentifiers3);

        RegistrationStudentEntity student3 = RegistrationStudentEntity.builder()
            .homeStudyRight(StudyRightEntity.fromStudyRight(studyRightToUpdate))
            .hostStudyRight(StudyRightEntity.fromStudyRight(hostStudyRight3))
            .build();

        registrationEntityForOtherOrg.setStudent(student3);
        registrationRepository.create(registrationEntityForOtherOrg);

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(HetuUtil.generateRandom());
        updateStudentStudyRightRequest.setOid(Oid.randomOid(OidPersonNode.PERSON));

        StudyRightStatus studyRightStatus = new StudyRightStatus();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ENDED);
        studyRightStatus.setStartDate(LocalDate.now());
        studyRightStatus.setEndDate(LocalDate.now().plusYears(1));

        StudyRightIdentifier studyRightToUpdateIdentifiers = new StudyRightIdentifier();
        studyRightToUpdateIdentifiers.setStudyRightId("OIK1");
        studyRightToUpdateIdentifiers.setOrganisationTkCodeReference(sendingOrganisation.getId());

        StudentStudyRight studentStudyRight = new StudentStudyRight();
        studentStudyRight.setIdentifiers(studyRightToUpdateIdentifiers);
        studentStudyRight.setEligibleForNetworkStudies(false);
        studentStudyRight.setStudyRightStatus(studyRightStatus);
        studentStudyRight.setStudyRightType(StudyRightType.BACHELOR);

        updateStudentStudyRightRequest.setHomeStudyRight(studentStudyRight);

        Message cancelResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentStudyRightRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(cancelResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        // Verify student entity statuses marked correctly
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightRequest receivedUpdateRequest =
            (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        // Test host study right set
        assertEquals(2, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i ->
                i.getStudyRightId().equals(hostStudyRightIdentifiers.getStudyRightId())
                    && i.getOrganisationTkCodeReference().equals(hostStudyRightIdentifiers.getOrganisationTkCodeReference())));
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i ->
                i.getStudyRightId().equals(hostStudyRightIdentifiers2.getStudyRightId())
                    && i.getOrganisationTkCodeReference().equals(hostStudyRightIdentifiers2.getOrganisationTkCodeReference())));

        // check if the organisation 2 as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation2 = jmsTemplate.receive(receivingOrganisation2.getQueue());
        receivedUpdateRequest = (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation2);

        // Test host study right set
        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i ->
                i.getStudyRightId().equals(hostStudyRightIdentifiers3.getStudyRightId())
                    && i.getOrganisationTkCodeReference().equals(hostStudyRightIdentifiers3.getOrganisationTkCodeReference())));
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_testNoRegistrationsFound_shouldFail() throws JMSException {
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(HetuUtil.generateRandom());
        updateStudentStudyRightRequest.setOid(Oid.randomOid(OidPersonNode.PERSON));

        StudyRightIdentifier studyRightIdentifier = new StudyRightIdentifier();
        studyRightIdentifier.setStudyRightId("OIK1");
        studyRightIdentifier.setOrganisationTkCodeReference(sendingOrganisation.getId());

        StudyRightStatus studyRightStatus = new StudyRightStatus();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ENDED);
        studyRightStatus.setStartDate(LocalDate.now());
        studyRightStatus.setEndDate(LocalDate.now().plusYears(1));

        StudentStudyRight studentStudyRight = new StudentStudyRight();
        studentStudyRight.setIdentifiers(studyRightIdentifier);
        studentStudyRight.setEligibleForNetworkStudies(false);
        studentStudyRight.setStudyRightStatus(studyRightStatus);
        studentStudyRight.setStudyRightType(StudyRightType.BACHELOR);

        updateStudentStudyRightRequest.setHomeStudyRight(studentStudyRight);

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentStudyRightRequest, sendingOrganisation.getId());
        DefaultResponse updateStudyRightResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateStudyRightResp.getStatus() == Status.FAILED);
        assertTrue(updateStudyRightResp.getMessage().contains(studyRightIdentifier.getStudyRightId()));
        assertTrue(updateStudyRightResp.getMessage().contains(studyRightIdentifier.getOrganisationTkCodeReference()));

        List<StudentEntity> studentEntities = studentRepository.findByOidOrPersonId(updateStudentStudyRightRequest.getOid(),
                updateStudentStudyRightRequest.getPersonId(), Pageable.unpaged());
        assertTrue(CollectionUtils.isEmpty(studentEntities));
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_rejectUpdateMessageWithReason_shouldSucceed() throws JMSException {
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

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
            "CN-1", new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("verkosto", null, null),
            Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            replySelections.add(sel);
        }

        RegistrationReplyRequest replyRequest = new RegistrationReplyRequest();
        replyRequest.setSelections(replySelections);
        replyRequest.setStatus(RegistrationStatus.REGISTERED);
        replyRequest.setHostStudyRight(req.getStudent().getHomeStudyRight());
        replyRequest.setHostStudentEppn("rara@rara.fi");
        replyRequest.setHostStudentNumber("21342143");
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());

        Message replyResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyRequest, receivingOrganisation.getId());
        DefaultResponse replyResponse = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(replyResponseMessage);
        assertTrue(replyResponse.getStatus() == Status.OK);

        // now we should have registration with different statuses in the repository
        List<RegistrationEntity> registrations = StreamSupport.stream(registrationRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, registrations.size());

        RegistrationEntity reg = registrations.get(0);

        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() ==
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        // Consume message from sending organisation queue (forwarded by sending registration status message)
        Message forwardedRegistrationStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(req.getStudent().getPersonId());
        updateStudentStudyRightRequest.setOid(req.getStudent().getOid());

        StudentStudyRight studentStudyRight = modelMapper.map(req.getStudent().getHomeStudyRight(), StudentStudyRight.class);
        studentStudyRight.setEligibleForNetworkStudies(false);
        updateStudentStudyRightRequest.setHomeStudyRight(studentStudyRight);

        Message cancelResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentStudyRightRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(cancelResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightRequest receivedUpdateRequest =
            (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        assertEquals(cancelResp.getStudentRequestId(), receivedUpdateRequest.getStudentRequestId());
        assertEquals(updateStudentStudyRightRequest.getOid(), receivedUpdateRequest.getOid());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            receivedUpdateRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getStudyRightId(),
            receivedUpdateRequest.getHomeStudyRight().getIdentifiers().getStudyRightId());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getStudyRightStatusValue());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getStartDate(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getStartDate());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightStatus().getEndDate(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightStatus().getEndDate());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightType(),
            receivedUpdateRequest.getHomeStudyRight().getStudyRightType());

        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getEligibleForNetworkStudies(),
            receivedUpdateRequest.getHomeStudyRight().getEligibleForNetworkStudies());

        assertEquals(1, receivedUpdateRequest.getHomeStudyRight().getKeywords().size());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getKey(),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getKey());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getKeySet(),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getKeySet());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("fi"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("fi"));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("en"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("en"));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("sv"),
            receivedUpdateRequest.getHomeStudyRight().getKeywords().get(0).getValue().getValue("sv"));

        // Test host study right set
        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertEquals(reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId(),
            receivedUpdateRequest.getHostStudyRightIdentifiers().get(0).getStudyRightId());
        assertEquals(reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            receivedUpdateRequest.getHostStudyRightIdentifiers().get(0).getOrganisationTkCodeReference());

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());
        assertTrue(StringUtils.isNotBlank(cancelResp.getStudentRequestId()));
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            studentEntities.get(0).getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getIdentifiers().getStudyRightId(),
            studentEntities.get(0).getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getStudyRightType(),
            studentEntities.get(0).getHomeStudyRight().getStudyRightType());
        assertEquals(updateStudentStudyRightRequest.getHomeStudyRight().getEligibleForNetworkStudies(),
            studentEntities.get(0).getHomeStudyRight().getEligibleForNetworkStudies());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType.UPDATE_STUDENT_STUDYRIGHT, studentEntities.get(0).getMessageType());
        assertEquals(sendingOrganisation.getId(), studentEntities.get(0).getHomeOrganisationTkCode());
        assertNotNull(studentEntities.get(0).getTimestamp());

        // Target organisation replies to message that they have received it
        StudentReplyRequest replyMessage = new UpdateStudentStudyRightReplyRequest();
        replyMessage.setStudentRequestId(receivedUpdateRequest.getStudentRequestId());
        replyMessage.setStatus(StudentStatus.REJECTED);
        replyMessage.setRejectionReason(new LocalisedString("Opiskeljaa ei löytynyt", "Could not find student", "Nej"));

        Message cancelStatusResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyMessage, receivingOrganisation.getId());
        DefaultResponse cancelStatusResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(cancelStatusResponseMessage);
        assertTrue(cancelStatusResp.getStatus() == Status.OK);

        // NOTE: Field statuses is not updated anymore in the UpdateStudenStudtRightReplyProcessor due to concurrency issues.
        studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        Message forwardedCancelStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightReplyRequest receivedForwardedCancelStatusMessage =
            (ForwardedUpdateStudentStudyRightReplyRequest) jmsTemplate.getMessageConverter().fromMessage(forwardedCancelStatusMessage);
        assertEquals(replyMessage.getStudentRequestId(), receivedForwardedCancelStatusMessage.getStudentRequestId());
        assertEquals(replyMessage.getStatus(), receivedForwardedCancelStatusMessage.getStatus());
        assertEquals(replyMessage.getRejectionReason().getValue("fi"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("fi"));
        assertEquals(replyMessage.getRejectionReason().getValue("en"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("en"));
        assertEquals(replyMessage.getRejectionReason().getValue("sv"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("sv"));
    }
}
