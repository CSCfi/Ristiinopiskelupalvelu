package fi.uta.ristiinopiskelu.handler.integration.route.current;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.*;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedCreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.student.*;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
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
import org.springframework.beans.factory.annotation.Value;
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
public class StudentRouteIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(StudentRouteIntegrationTest.class);

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

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        jmsTemplate.setReceiveTimeout(500000);
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);
    }

    @Test
    public void testSendingUpdateStudentRequest_shouldSucceed() throws JMSException {
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
                "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(), reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        // Consume message from sending organisation queue (forwarded by sending registration status message)
        Message forwardedRegistrationStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());

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

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        StudentResponse updateResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentRequest receivedUpdateRequest = (ForwardedUpdateStudentRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);
        assertEquals(updateResp.getStudentRequestId(), receivedUpdateRequest.getStudentRequestId());
        assertEquals(updateRequest.getOid(), receivedUpdateRequest.getOid());
        assertEquals(updateRequest.getCountryOfCitizenship(), receivedUpdateRequest.getCountryOfCitizenship());
        assertEquals(updateRequest.getFirstNames(), receivedUpdateRequest.getFirstNames());

        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers(), receivedUpdateRequest.getHostStudyRightIdentifiers().get(0));

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());
        assertEquals(updateRequest.getPersonId(), studentEntities.get(0).getPersonId());
        assertEquals(updateRequest.getOid(), studentEntities.get(0).getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntities.get(0).getFirstNames());
        assertEquals(1, studentEntities.size());
        assertEquals(updateRequest.getPhone().get(0).getNumber(), studentEntities.get(0).getPhone().get(0).getNumber());
        assertEquals(updateRequest.getPhone().get(0).getDescription(), studentEntities.get(0).getPhone().get(0).getDescription());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(req.getStudent().getPersonId(), studentEntity.getPersonId());
        assertEquals(req.getStudent().getOid(), studentEntity.getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntity.getFirstNames());

        // Target organisation replies to message that they have received it
        StudentReplyRequest updateReplyMessage = new UpdateStudentReplyRequest();
        updateReplyMessage.setStudentRequestId(receivedUpdateRequest.getStudentRequestId());
        updateReplyMessage.setStatus(StudentStatus.UPDATED);

        Message updateStatusResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateReplyMessage, receivingOrganisation.getId());
        DefaultResponse updateStatusResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateStatusResponseMessage);
        assertTrue(updateStatusResp.getStatus() == Status.OK);

        studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        Message forwardedUpdateStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedUpdateStudentReplyRequest receivedForwardedUpdateStatusMessage = (ForwardedUpdateStudentReplyRequest) jmsTemplate.getMessageConverter().fromMessage(forwardedUpdateStatusMessage);
        assertEquals(updateReplyMessage.getStudentRequestId(), receivedForwardedUpdateStatusMessage.getStudentRequestId());
        assertEquals(updateReplyMessage.getStatus(), receivedForwardedUpdateStatusMessage.getStatus());
    }

    @Test
    public void testSendingUpdateStudentRequestWithWarnings_shouldSucceed() throws JMSException {
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
                "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, networkEntity.getId());
        registrationRepository.create(registrationEntity);

        // send student update request with warnings
        String studentOid = registrationEntity.getStudent().getOid();
        String studentPersonId = registrationEntity.getStudent().getPersonId();

        StudentWarning studentWarning = new StudentWarning();
        studentWarning.setType(StudentWarningType.NOT_ELIGIBLE_FOR_CROSS_STUDIES);
        studentWarning.setStartDate(LocalDate.now().minusDays(1));
        studentWarning.setEndDate(LocalDate.now().plusDays(1));

        UpdateStudentRequest updateRequest = new UpdateStudentRequest();
        updateRequest.setPersonId(studentPersonId);
        updateRequest.setOid(studentOid);
        updateRequest.setFirstNames("Teppo");
        updateRequest.setSurName("Testaaja");
        updateRequest.setWarnings(Collections.singletonList(studentWarning));

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        StudentResponse updateResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(updateRequest.getPersonId(), studentEntity.getPersonId());
        assertEquals(updateRequest.getOid(), studentEntity.getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntity.getFirstNames());
        assertEquals(updateRequest.getSurName(), studentEntity.getSurName());
        assertNotNull(studentEntity.getWarnings());
        assertEquals(1, studentEntity.getWarnings().size());

        // Receive forwarded message but we can just ignore it since this test is not about it
        Message forwardedMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());

        // now send a registration request
        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
        req.getStudent().setPersonId(studentPersonId);
        req.getStudent().setOid(studentOid);
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        // now check if the warnings were received in receiving organisation
        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        assertNotNull(receivedRequest.getWarnings());
        assertEquals(1, receivedRequest.getWarnings().size());
        assertEquals(StudentWarningType.NOT_ELIGIBLE_FOR_CROSS_STUDIES, receivedRequest.getWarnings().get(0).getType());
        assertEquals(studentWarning.getStartDate(), receivedRequest.getWarnings().get(0).getStartDate());
        assertEquals(studentWarning.getEndDate(), receivedRequest.getWarnings().get(0).getEndDate());
    }

    @Test
    public void testSendingUpdateStudentRequestWithPreviousSchemaVersion_shouldSucceed() throws JMSException {
        int previousSchemaVersion = this.messageSchemaVersion - 1;

        JmsHelper.setMessageSchemaVersion(previousSchemaVersion);

        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
            "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), previousSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, networkEntity.getId());
        registrationRepository.create(registrationEntity);

        // send student update request with warnings
        String studentOid = registrationEntity.getStudent().getOid();
        String studentPersonId = registrationEntity.getStudent().getPersonId();

        fi.uta.ristiinopiskelu.messaging.message.v8.student.UpdateStudentRequest updateRequest = new fi.uta.ristiinopiskelu.messaging.message.v8.student.UpdateStudentRequest();
        updateRequest.setPersonId(studentPersonId);
        updateRequest.setOid(studentOid);
        updateRequest.setFirstNames("Teppo");
        updateRequest.setSurName("Testaaja");

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        fi.uta.ristiinopiskelu.messaging.message.v8.student.StudentResponse updateResp =
            (fi.uta.ristiinopiskelu.messaging.message.v8.student.StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequestWithPreviousSchemaVersion_shouldSucceed() throws JMSException {
        int previousSchemaVersion = this.messageSchemaVersion - 1;

        JmsHelper.setMessageSchemaVersion(previousSchemaVersion);

        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
            "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), previousSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, networkEntity.getId());
        registrationRepository.create(registrationEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight homeStudyRight = DtoInitializerV8.getStudentStudyRight(sendingOrganisation.getId(), true);
        String studentOid = registrationEntity.getStudent().getOid();
        String studentPersonId = registrationEntity.getStudent().getPersonId();
      
        fi.uta.ristiinopiskelu.messaging.message.v8.student.UpdateStudentStudyRightRequest updateRequest = new fi.uta.ristiinopiskelu.messaging.message.v8.student.UpdateStudentStudyRightRequest();
        updateRequest.setPersonId(studentPersonId);
        updateRequest.setOid(studentOid);
        updateRequest.setHomeStudyRight(homeStudyRight);

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        fi.uta.ristiinopiskelu.messaging.message.v8.student.StudentResponse updateResp =
            (fi.uta.ristiinopiskelu.messaging.message.v8.student.StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == fi.uta.ristiinopiskelu.messaging.message.v8.Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));
    }

    @Test
    public void testSendingUpdateStudentRequestWithExpiredWarnings_shouldSucceed() throws JMSException {
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
                "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, networkEntity.getId());
        registrationRepository.create(registrationEntity);

        // send student update request with warnings
        String studentOid = registrationEntity.getStudent().getOid();
        String studentPersonId = registrationEntity.getStudent().getPersonId();

        StudentWarning studentWarning = new StudentWarning();
        studentWarning.setType(StudentWarningType.NOT_ELIGIBLE_FOR_CROSS_STUDIES);
        studentWarning.setStartDate(LocalDate.now().minusDays(30));
        studentWarning.setEndDate(LocalDate.now().minusDays(15));

        UpdateStudentRequest updateRequest = new UpdateStudentRequest();
        updateRequest.setPersonId(studentPersonId);
        updateRequest.setOid(studentOid);
        updateRequest.setFirstNames("Teppo");
        updateRequest.setSurName("Testaaja");
        updateRequest.setWarnings(Collections.singletonList(studentWarning));

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        StudentResponse updateResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(updateRequest.getPersonId(), studentEntity.getPersonId());
        assertEquals(updateRequest.getOid(), studentEntity.getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntity.getFirstNames());
        assertEquals(updateRequest.getSurName(), studentEntity.getSurName());
        assertNotNull(studentEntity.getWarnings());
        assertEquals(1, studentEntity.getWarnings().size());

        // Receive forwarded message but we can just ignore it since this test is not about it
        Message forwardedMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());

        // now send a registration request
        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
        req.getStudent().setPersonId(studentPersonId);
        req.getStudent().setOid(studentOid);
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));

        // now check if the warnings were received in receiving organisation
        Message messageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedCreateRegistrationRequest receivedRequest = (ForwardedCreateRegistrationRequest) jmsTemplate.getMessageConverter().fromMessage(messageReceivedInOrganisation);

        assertNotNull(receivedRequest);
        assertEquals(resp.getRegistrationRequestId(), receivedRequest.getRegistrationRequestId());
        assertEquals(req.getStudent().getFirstNames(), receivedRequest.getStudent().getFirstNames());
        assertEquals(req.getStudent().getHomeEppn(), receivedRequest.getStudent().getHomeEppn());
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(req.getSelections().get(0).getSelectionItemId(), receivedRequest.getSelections().get(0).getSelectionItemId());
        assertEquals(DateUtils.getFormatted(req.getEnrolmentDateTime()), DateUtils.getFormatted(receivedRequest.getEnrolmentDateTime()));

        assertNotNull(receivedRequest.getWarnings());
        assertEquals(0, receivedRequest.getWarnings().size());
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_shouldSucceed() throws JMSException {
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
                "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
                "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED));
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
        assertEquals(StudentMessageType.UPDATE_STUDENT_STUDYRIGHT, studentEntities.get(0).getMessageType());
        assertEquals(sendingOrganisation.getId(), studentEntities.get(0).getHomeOrganisationTkCode());
        assertNotNull(studentEntities.get(0).getTimestamp());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(req.getStudent().getPersonId(), studentEntity.getPersonId());
        assertEquals(req.getStudent().getOid(), studentEntity.getOid());

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
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation2 = EntityInitializer.getOrganisationEntity(
            "TESTORG3", "testiorganisaatio3", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);
        organisationRepository.create(receivingOrganisation2);

        StudyRightIdentifier homeStudyRightIdentifiers = new StudyRightIdentifier();
        homeStudyRightIdentifiers.setStudyRightId("OIK1");
        homeStudyRightIdentifiers.setOrganisationTkCodeReference(sendingOrganisation.getId());

        StudyRight studyRightToUpdate = new StudyRight();
        studyRightToUpdate.setIdentifiers(homeStudyRightIdentifiers);

        // Create multiple host study rights for host
        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers = new StudyRightIdentifier();
        hostStudyRightIdentifiers.setStudyRightId("OIK2");
        hostStudyRightIdentifiers.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRight hostStudyRight = new StudyRight();
        hostStudyRight.setIdentifiers(hostStudyRightIdentifiers);

        ExtendedStudent student = new ExtendedStudent();
        student.setHomeStudyRight(studyRightToUpdate);
        student.setHostStudyRight(hostStudyRight);

        registrationEntity.setStudent(student);
        registrationRepository.create(registrationEntity);

        RegistrationEntity registrationEntity2 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers2 = new StudyRightIdentifier();
        hostStudyRightIdentifiers2.setStudyRightId("OIK3");
        hostStudyRightIdentifiers2.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRight hostStudyRight2 = new StudyRight();
        hostStudyRight2.setIdentifiers(hostStudyRightIdentifiers2);

        ExtendedStudent student2 = new ExtendedStudent();
        student2.setHomeStudyRight(studyRightToUpdate);
        student2.setHostStudyRight(hostStudyRight2);

        registrationEntity2.setStudent(student2);
        registrationRepository.create(registrationEntity2);

        // Create one registration with same study right identifiers as first (OIK2) (should not be duplicated)
        RegistrationEntity registrationEntityWithSameStudyRightsAs1 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTRATION_REJECTED, null);

        registrationEntityWithSameStudyRightsAs1.setStudent(student);
        registrationRepository.create(registrationEntityWithSameStudyRightsAs1);

        // Create one registration for receivingOrganisation 2
        RegistrationEntity registrationEntityForOtherOrg = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation2.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers3 = new StudyRightIdentifier();
        hostStudyRightIdentifiers3.setStudyRightId("OIK4");
        hostStudyRightIdentifiers3.setOrganisationTkCodeReference(receivingOrganisation2.getId());

        StudyRight hostStudyRight3 = new StudyRight();
        hostStudyRight3.setIdentifiers(hostStudyRightIdentifiers3);

        ExtendedStudent student3 = new ExtendedStudent();
        student3.setHomeStudyRight(studyRightToUpdate);
        student3.setHostStudyRight(hostStudyRight3);

        registrationEntityForOtherOrg.setStudent(student3);
        registrationRepository.create(registrationEntityForOtherOrg);

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(HetuUtil.generateRandom());
        updateStudentStudyRightRequest.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

        StudyRightStatus studyRightStatus = new StudyRightStatus();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ENDED);
        studyRightStatus.setStartDate(LocalDate.now());
        studyRightStatus.setEndDate(LocalDate.now().plusYears(1));

        StudentStudyRight studentStudyRight = new StudentStudyRight();
        studentStudyRight.setIdentifiers(studyRightToUpdate.getIdentifiers());
        studentStudyRight.setEligibleForNetworkStudies(false);
        studentStudyRight.setStudyRightStatus(studyRightStatus);
        studentStudyRight.setStudyRightType(StudyRightType.BACHELOR);

        updateStudentStudyRightRequest.setHomeStudyRight(studentStudyRight);

        Message cancelResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentStudyRightRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(cancelResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightRequest receivedUpdateRequest =
            (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        // Test host study right set
        assertEquals(2, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers)));
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers2)));

        // check if the organisation 2 as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation2 = jmsTemplate.receive(receivingOrganisation2.getQueue());
        receivedUpdateRequest = (ForwardedUpdateStudentStudyRightRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation2);

        // Test host study right set
        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers3)));
    }

    @Test
    public void testSendingUpdateStudentRequest_testOnlySendingOrganisationStudyRightsUpdated_shouldSucceed() throws JMSException {

        // create 4 organisations. same student is doing cross studies through two different universities.
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity sendingOrganisation2 = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Lähettävä testiorganisaatio 2", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG3", "testiorganisaatio3", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation2 = EntityInitializer.getOrganisationEntity(
            "TESTORG4", "testiorganisaatio4", new LocalisedString("Vastaanottava testiorganisaatio 2", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(sendingOrganisation2);
        organisationRepository.create(receivingOrganisation);
        organisationRepository.create(receivingOrganisation2);

        // the first student/study right
        StudyRightIdentifier homeStudyRightIdentifiers = new StudyRightIdentifier();
        homeStudyRightIdentifiers.setStudyRightId("OIK1");
        homeStudyRightIdentifiers.setOrganisationTkCodeReference(sendingOrganisation.getId());

        StudyRight homeStudyRight = new StudyRight();
        homeStudyRight.setIdentifiers(homeStudyRightIdentifiers);

        StudyRightIdentifier hostStudyRightIdentifiers = new StudyRightIdentifier();
        hostStudyRightIdentifiers.setStudyRightId("OIK2");
        hostStudyRightIdentifiers.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRight hostStudyRight = new StudyRight();
        hostStudyRight.setIdentifiers(hostStudyRightIdentifiers);

        ExtendedStudent student = new ExtendedStudent();
        student.setHomeStudyRight(homeStudyRight);
        student.setHostStudyRight(hostStudyRight);
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        student.setPersonId(HetuUtil.generateRandom());

        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, null);

        registrationEntity.setStudent(student);
        registrationRepository.create(registrationEntity);

        // the second student/study right
        StudyRightIdentifier homeStudyRightIdentifiers2 = new StudyRightIdentifier();
        homeStudyRightIdentifiers2.setStudyRightId("OIK2");
        homeStudyRightIdentifiers2.setOrganisationTkCodeReference(sendingOrganisation2.getId());

        StudyRight homeStudyRight2 = new StudyRight();
        homeStudyRight2.setIdentifiers(homeStudyRightIdentifiers2);

        StudyRightIdentifier hostStudyRightIdentifiers2 = new StudyRightIdentifier();
        hostStudyRightIdentifiers2.setStudyRightId("OIK3");
        hostStudyRightIdentifiers2.setOrganisationTkCodeReference(receivingOrganisation2.getId());

        StudyRight hostStudyRight2 = new StudyRight();
        hostStudyRight2.setIdentifiers(hostStudyRightIdentifiers2);

        // note, this is the same student with exact same oid and person id as in the first registration but different universities
        ExtendedStudent student2 = new ExtendedStudent();
        student2.setHomeStudyRight(homeStudyRight2);
        student2.setHostStudyRight(hostStudyRight2);
        student2.setOid(student.getOid());
        student2.setPersonId(student.getPersonId());

        RegistrationEntity registrationEntity2 = EntityInitializer.getRegistrationEntity(sendingOrganisation2.getId(), receivingOrganisation2.getId(),
            null, RegistrationStatus.REGISTERED, null);

        registrationEntity2.setStudent(student2);
        registrationRepository.create(registrationEntity2);

        // now send update study right message through the first organisation. therefore only organisations made through this should receive the update.
        UpdateStudentRequest updateStudentRequest = new UpdateStudentRequest();
        updateStudentRequest.setOid(student.getOid());
        updateStudentRequest.setPersonId(student.getPersonId());
        updateStudentRequest.setCountryOfCitizenship(Collections.singletonList(Country.AI));
        updateStudentRequest.setFirstNames("Jaska");
        updateStudentRequest.setSurName("Jokunen");

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        // Status at this point:
        // Student has two home organizations, ORG1 (OIK1) and ORG2 (OIK2)
        // She has registered to two other schools, ORG3 (OIK2) from ORG1, and ORG4 (OIK3) from ORG2
        // Student info has been updated at ORG1. This update should be delivered (only) to ORG3.

        // Verify that we have one student entity (due to one update)
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        // Check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentRequest receivedUpdateRequest =
            (ForwardedUpdateStudentRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        // Test host study right set, and some other fields
        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers)));
        assertEquals(updateStudentRequest.getFirstNames(), receivedUpdateRequest.getFirstNames());
        assertEquals(updateStudentRequest.getSurName(), receivedUpdateRequest.getSurName());

        // make sure that the second receivingOrganisation did not get any updates
        jmsTemplate.setReceiveTimeout(1000);

        Message updateMessageReceivedInOrganisation2 = jmsTemplate.receive(receivingOrganisation2.getQueue());
        assertNull(updateMessageReceivedInOrganisation2);
    }

    @Test
    public void testSendingUpdateStudentRequest_testOnlyHostStudyRightsForwardedCorrectly_shouldSucceed() throws JMSException {
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation2 = EntityInitializer.getOrganisationEntity(
            "TESTORG3", "testiorganisaatio3", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);
        organisationRepository.create(receivingOrganisation2);

        StudyRightIdentifier homeStudyRightIdentifiers = new StudyRightIdentifier();
        homeStudyRightIdentifiers.setStudyRightId("OIK1");
        homeStudyRightIdentifiers.setOrganisationTkCodeReference(sendingOrganisation.getId());

        StudyRight studyRightToUpdate = new StudyRight();
        studyRightToUpdate.setIdentifiers(homeStudyRightIdentifiers);

        // Create multiple host study rights for host
        RegistrationEntity registrationEntity = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers = new StudyRightIdentifier();
        hostStudyRightIdentifiers.setStudyRightId("OIK2");
        hostStudyRightIdentifiers.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRight hostStudyRight = new StudyRight();
        hostStudyRight.setIdentifiers(hostStudyRightIdentifiers);

        ExtendedStudent student = new ExtendedStudent();
        student.setHomeStudyRight(studyRightToUpdate);
        student.setHostStudyRight(hostStudyRight);
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        student.setPersonId(HetuUtil.generateRandom());

        registrationEntity.setStudent(student);
        registrationRepository.create(registrationEntity);

        RegistrationEntity registrationEntity2 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers2 = new StudyRightIdentifier();
        hostStudyRightIdentifiers2.setStudyRightId("OIK3");
        hostStudyRightIdentifiers2.setOrganisationTkCodeReference(receivingOrganisation.getId());

        StudyRight hostStudyRight2 = new StudyRight();
        hostStudyRight2.setIdentifiers(hostStudyRightIdentifiers2);

        ExtendedStudent student2 = new ExtendedStudent();
        student2.setHomeStudyRight(studyRightToUpdate);
        student2.setHostStudyRight(hostStudyRight2);
        student2.setOid(student.getOid());
        student2.setPersonId(student.getPersonId());

        registrationEntity2.setStudent(student2);
        registrationRepository.create(registrationEntity2);

        // Create one registration with same study right identifiers as first (OIK2) (should not be duplicated)
        RegistrationEntity registrationEntityWithSameStudyRightsAs1 = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            null, RegistrationStatus.REGISTRATION_REJECTED, null);

        registrationEntityWithSameStudyRightsAs1.setStudent(student);
        registrationRepository.create(registrationEntityWithSameStudyRightsAs1);

        // Create one registration for receivingOrganisation 2
        RegistrationEntity registrationEntityForOtherOrg = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation2.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers3 = new StudyRightIdentifier();
        hostStudyRightIdentifiers3.setStudyRightId("OIK4");
        hostStudyRightIdentifiers3.setOrganisationTkCodeReference(receivingOrganisation2.getId());

        StudyRight hostStudyRight3 = new StudyRight();
        hostStudyRight3.setIdentifiers(hostStudyRightIdentifiers3);

        ExtendedStudent student3 = new ExtendedStudent();
        student3.setHomeStudyRight(studyRightToUpdate);
        student3.setHostStudyRight(hostStudyRight3);
        student3.setOid(student.getOid());
        student3.setPersonId(student.getPersonId());

        registrationEntityForOtherOrg.setStudent(student3);
        registrationRepository.create(registrationEntityForOtherOrg);

        // Create one registration for receivingOrganisation 2 with different personId (to test searching by personId OR oid clause work)
        RegistrationEntity registrationEntityForOtherOrgWithOtherPersonId = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation2.getId(),
            null, RegistrationStatus.REGISTERED, null);

        StudyRightIdentifier hostStudyRightIdentifiers4 = new StudyRightIdentifier();
        hostStudyRightIdentifiers4.setStudyRightId("OIK5");
        hostStudyRightIdentifiers4.setOrganisationTkCodeReference(receivingOrganisation2.getId());

        StudyRight hostStudyRight4 = new StudyRight();
        hostStudyRight4.setIdentifiers(hostStudyRightIdentifiers4);

        ExtendedStudent student4 = new ExtendedStudent();
        student4.setHomeStudyRight(studyRightToUpdate);
        student4.setHostStudyRight(hostStudyRight4);
        student4.setOid(null);
        student4.setPersonId(HetuUtil.generateRandom());

        registrationEntityForOtherOrgWithOtherPersonId.setStudent(student4);
        registrationRepository.create(registrationEntityForOtherOrgWithOtherPersonId);

        // now send update study right message
        UpdateStudentRequest updateStudentRequest = new UpdateStudentRequest();
        updateStudentRequest.setOid(registrationEntity.getStudent().getOid());
        updateStudentRequest.setPersonId(registrationEntityForOtherOrgWithOtherPersonId.getStudent().getPersonId());
        updateStudentRequest.setCountryOfCitizenship(Collections.singletonList(Country.AI));
        updateStudentRequest.setFirstNames("Jaska");
        updateStudentRequest.setSurName("Jokunen");

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentRequest, sendingOrganisation.getId());
        StudentResponse cancelResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(cancelResp.getStatus() == Status.OK);

        // Status at this point
        // Student of ORG1 with Oid1 and PersonId1, having home study right OIK1, has the following registrations:
        // - 2x to ORG2 using host study right OIK2
        // - To ORG2 using host study right OIK3
        // - To ORG3 using host study right OIK4
        // The same student (ORG1, OIK1) has also the following registration, but with different PersonId2 and no Oid:
        // - To ORG3 using host study right OIK5
        // Then ORG1 sends update with Oid1 and PersonId2.

        // The update concerns all registrations (first four due to Oid1 match and the fifth one due to PersonId2 match).

        // Check that the entity regarding the update message was created
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentRequest receivedUpdateRequest =
            (ForwardedUpdateStudentRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);

        // Test host study right set
        assertEquals(2, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers))); // OIK2
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers2))); // OIK3

        // check if the organisation 2 as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation2 = jmsTemplate.receive(receivingOrganisation2.getQueue());
        receivedUpdateRequest = (ForwardedUpdateStudentRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation2);

        // Test host study right set
        assertEquals(2, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers3))); // OIK4
        assertTrue(receivedUpdateRequest.getHostStudyRightIdentifiers().stream().anyMatch(i -> i.equals(hostStudyRightIdentifiers4))); // OIK5
    }

    @Test
    public void testSendingUpdateStudentRequest_testNoRegistrationsFound_shouldFail() throws JMSException {
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);

        UpdateStudentRequest updateStudentRequest = new UpdateStudentRequest();
        updateStudentRequest.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        updateStudentRequest.setPersonId(HetuUtil.generateRandom());
        updateStudentRequest.setCountryOfCitizenship(Collections.singletonList(Country.AI));
        updateStudentRequest.setFirstNames("Jaska");
        updateStudentRequest.setSurName("Jokunen");

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudentRequest, sendingOrganisation.getId());
        DefaultResponse updateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == Status.FAILED);
        assertTrue(updateResp.getMessage().contains("Stopped handling update student -message"));

        List<StudentEntity> studentEntities = studentRepository.findByOidOrPersonId(updateStudentRequest.getOid(),
                updateStudentRequest.getPersonId(), Pageable.unpaged());
        assertTrue(CollectionUtils.isEmpty(studentEntities));
    }

    @Test
    public void testSendingUpdateStudentStudyRightRequest_testNoRegistrationsFound_shouldFail() throws JMSException {
        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);

        // now send update study right message
        UpdateStudentStudyRightRequest updateStudentStudyRightRequest = new UpdateStudentStudyRightRequest();
        updateStudentStudyRightRequest.setPersonId(HetuUtil.generateRandom());
        updateStudentStudyRightRequest.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

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
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
            "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED));
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
        assertEquals(StudentMessageType.UPDATE_STUDENT_STUDYRIGHT, studentEntities.get(0).getMessageType());
        assertEquals(sendingOrganisation.getId(), studentEntities.get(0).getHomeOrganisationTkCode());
        assertNotNull(studentEntities.get(0).getTimestamp());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(req.getStudent().getPersonId(), studentEntity.getPersonId());
        assertEquals(req.getStudent().getOid(), studentEntity.getOid());

        // Target organisation replies to message that they have received it
        StudentReplyRequest replyMessage = new UpdateStudentStudyRightReplyRequest();
        replyMessage.setStudentRequestId(receivedUpdateRequest.getStudentRequestId());
        replyMessage.setStatus(StudentStatus.REJECTED);
        replyMessage.setRejectionReason(new LocalisedString("Opiskeljaa ei löytynyt", "Could not find student", "Nej"));

        Message cancelStatusResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, replyMessage, receivingOrganisation.getId());
        DefaultResponse cancelStatusResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(cancelStatusResponseMessage);
        assertTrue(cancelStatusResp.getStatus() == Status.OK);

        studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        // Finally ensure that the original update sender received the REJECTED reply.

        Message forwardedCancelStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedUpdateStudentStudyRightReplyRequest receivedForwardedCancelStatusMessage =
            (ForwardedUpdateStudentStudyRightReplyRequest) jmsTemplate.getMessageConverter().fromMessage(forwardedCancelStatusMessage);
        assertEquals(replyMessage.getStudentRequestId(), receivedForwardedCancelStatusMessage.getStudentRequestId());
        assertEquals(replyMessage.getStatus(), receivedForwardedCancelStatusMessage.getStatus());
        assertEquals(replyMessage.getRejectionReason().getValue("fi"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("fi"));
        assertEquals(replyMessage.getRejectionReason().getValue("en"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("en"));
        assertEquals(replyMessage.getRejectionReason().getValue("sv"), receivedForwardedCancelStatusMessage.getRejectionReason().getValue("sv"));
    }

    @Test
    public void testSendingUpdateStudentRequest_rejectUpdateMessageWithReason_shouldSucceed() throws JMSException {
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TESTORG1");
        organisation.setValidityInNetwork(validity);

        NetworkOrganisation organisation2 = new NetworkOrganisation();
        organisation2.setOrganisationTkCode("TESTORG2");
        organisation2.setValidityInNetwork(validity);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(
            "CN-1", new LocalisedString("verkosto", null, null), Arrays.asList(organisation, organisation2), validity, true);
        networkRepository.create(networkEntity);

        OrganisationEntity sendingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG1", "testiorganisaatio1", new LocalisedString("Lähettävä testiorganisaatio", null, null), this.messageSchemaVersion);

        OrganisationEntity receivingOrganisation = EntityInitializer.getOrganisationEntity(
            "TESTORG2", "testiorganisaatio2", new LocalisedString("Vastaanottava testiorganisaatio", null, null), this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
            "OJ1", "OJ1-CODE", receivingOrganisation.getId(), Collections.singletonList(cooperationNetwork), new LocalisedString("Opintojakso", null, null));
        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), "CN-1");
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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(), reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        // Consume message from sending organisation queue (forwarded by sending registration status message)
        Message forwardedRegistrationStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());

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

        Message updateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateRequest, sendingOrganisation.getId());
        StudentResponse updateResp = (StudentResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updateResp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(updateResp.getStudentRequestId()));

        // check if the organisation as a receiver in registration request actually got the update message
        Message updateMessageReceivedInOrganisation = jmsTemplate.receive(receivingOrganisation.getQueue());
        ForwardedUpdateStudentRequest receivedUpdateRequest = (ForwardedUpdateStudentRequest) jmsTemplate.getMessageConverter().fromMessage(updateMessageReceivedInOrganisation);
        assertEquals(updateResp.getStudentRequestId(), receivedUpdateRequest.getStudentRequestId());
        assertEquals(updateRequest.getOid(), receivedUpdateRequest.getOid());
        assertEquals(updateRequest.getCountryOfCitizenship(), receivedUpdateRequest.getCountryOfCitizenship());
        assertEquals(updateRequest.getFirstNames(), receivedUpdateRequest.getFirstNames());

        assertEquals(1, receivedUpdateRequest.getHostStudyRightIdentifiers().size());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers(), receivedUpdateRequest.getHostStudyRightIdentifiers().get(0));

        // now we should only one studententity with updated info
        List<StudentEntity> studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());
        assertEquals(updateRequest.getPersonId(), studentEntities.get(0).getPersonId());
        assertEquals(updateRequest.getOid(), studentEntities.get(0).getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntities.get(0).getFirstNames());
        assertEquals(1, studentEntities.size());
        assertEquals(updateRequest.getPhone().get(0).getNumber(), studentEntities.get(0).getPhone().get(0).getNumber());
        assertEquals(updateRequest.getPhone().get(0).getDescription(), studentEntities.get(0).getPhone().get(0).getDescription());

        StudentEntity studentEntity = studentEntities.get(0);
        assertEquals(req.getStudent().getPersonId(), studentEntity.getPersonId());
        assertEquals(req.getStudent().getOid(), studentEntity.getOid());
        assertEquals(updateRequest.getFirstNames(), studentEntity.getFirstNames());

        // Target organisation replies to message that they have received it
        StudentReplyRequest updateReplyMessage = new UpdateStudentReplyRequest();
        updateReplyMessage.setStudentRequestId(receivedUpdateRequest.getStudentRequestId());
        updateReplyMessage.setStatus(StudentStatus.UPDATED);
        updateReplyMessage.setRejectionReason(new LocalisedString("Opiskeljaa ei löytynyt", "Could not find student", "Nej"));

        Message updateStatusResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateReplyMessage, receivingOrganisation.getId());
        DefaultResponse updateStatusResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateStatusResponseMessage);
        assertTrue(updateStatusResp.getStatus() == Status.OK);

        studentEntities = StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, studentEntities.size());

        Message forwardedUpdateStatusMessage = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedUpdateStudentReplyRequest receivedForwardedUpdateStatusMessage = (ForwardedUpdateStudentReplyRequest) jmsTemplate.getMessageConverter().fromMessage(forwardedUpdateStatusMessage);
        assertEquals(updateReplyMessage.getStudentRequestId(), receivedForwardedUpdateStatusMessage.getStudentRequestId());
        assertEquals(updateReplyMessage.getStatus(), receivedForwardedUpdateStatusMessage.getStatus());
        assertEquals(updateReplyMessage.getRejectionReason().getValue("fi"), receivedForwardedUpdateStatusMessage.getRejectionReason().getValue("fi"));
        assertEquals(updateReplyMessage.getRejectionReason().getValue("en"), receivedForwardedUpdateStatusMessage.getRejectionReason().getValue("en"));
        assertEquals(updateReplyMessage.getRejectionReason().getValue("sv"), receivedForwardedUpdateStatusMessage.getRejectionReason().getValue("sv"));
    }
}
