package fi.uta.ristiinopiskelu.handler.integration.route.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.Rank;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.*;
import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.*;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

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
public class RegistrationRouteV8IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationRouteV8IntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(5000);
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
    private RealisationRepository realisationRepository;

    @Autowired
    private ModelMapper modelMapper;

    private int messageSchemaVersion = 8;

    private NetworkEntity testNetwork;
    private OrganisationEntity sendingOrganisation;
    private OrganisationEntity receivingOrganisation;

    @BeforeEach
    public void before() {
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
        sendingOrganisation.setOrganisationName(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Lähettävä testiorganisaatio", null, null));
        sendingOrganisation.setId("TESTORG1");
        sendingOrganisation.setQueue("testiorganisaatio1");
        sendingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        receivingOrganisation = new OrganisationEntity();
        receivingOrganisation.setOrganisationName(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Vastaanottava testiorganisaatio", null, null));
        receivingOrganisation.setId("TESTORG2");
        receivingOrganisation.setQueue("testiorganisaatio2");
        receivingOrganisation.setSchemaVersion(this.messageSchemaVersion);

        organisationRepository.create(sendingOrganisation);
        organisationRepository.create(receivingOrganisation);
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessage_shouldSucceed() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            sel.setRank(selectionRank);
            replySelections.add(sel);

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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(),
                reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getStudyRightId(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() ==
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getRank().getRankAll() == selectionRank.getRankAll() &&
            s.getRank().getMaxSeatsAll() == selectionRank.getMaxSeatsAll() &&
            s.getRank().getMaxSeatsCrossStudy() == selectionRank.getMaxSeatsCrossStudy() &&
            s.getRank().getRankCrossStudy() == selectionRank.getRankCrossStudy()));
    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessageWithoutStudentEppn_shouldSucceed() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            sel.setRank(selectionRank);
            replySelections.add(sel);

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

        assertEquals(req.getStudent().getOid(), reg.getStudent().getOid());
        assertEquals(req.getStudent().getFirstNames(), reg.getStudent().getFirstNames());
        assertEquals(req.getStudent().getSurName(), reg.getStudent().getSurName());
        assertEquals(req.getStudent().getHomeEppn(), reg.getStudent().getHomeEppn());
        assertEquals(req.getStudent().getDateOfBirth(), reg.getStudent().getDateOfBirth());
        assertEquals(req.getStudent().getGender(), reg.getStudent().getGender());
        assertEquals(req.getStudent().getMotherTongue(), reg.getStudent().getMotherTongue());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getStudyRightId(),
            reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
            reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() ==
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getRank().getRankAll() == selectionRank.getRankAll() &&
            s.getRank().getMaxSeatsAll() == selectionRank.getMaxSeatsAll() &&
            s.getRank().getMaxSeatsCrossStudy() == selectionRank.getMaxSeatsCrossStudy() &&
            s.getRank().getRankCrossStudy() == selectionRank.getRankCrossStudy()));

    }

    @Test
    public void testSendingCourseUnitRegistrationStatusMessageWithoutStudentnumber_shouldFail() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for (RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            sel.setRank(selectionRank);
            replySelections.add(sel);

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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for (RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
            sel.setRank(selectionRank);
            replySelections.add(sel);

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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        List<RegistrationSelection> replySelections = new ArrayList<>();
        for (RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.REJECTED);
            sel.setSelectionItemStatusInfo("Ilmoittautuminen hylätty.");
            sel.setRank(selectionRank);
            replySelections.add(sel);

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
    public void testSendingRealisationRegistration_shouldSuccess() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());


        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection groupSelection = DtoInitializer.getSelection(
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetusryhmä 1", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionType.CHOOSE_ONE,
            Arrays.asList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Tentti", null, null), "TESTIRYHMA1", null),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, List.of("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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

        RegistrationSelection requestRealisationSelection = req.getSelections().get(0).getParent();
        RegistrationSelection receivedRealisationSelection = receivedRequest.getSelections().get(0).getParent();
        RegistrationSelection requestCourseUnitSelection = req.getSelections().get(0).getParent();
        RegistrationSelection receivedCourseUnitSelection = receivedRequest.getSelections().get(0).getParent();

        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(requestRealisationSelection.getSelectionItemId(), receivedRealisationSelection.getSelectionItemId());
        assertEquals(requestRealisationSelection.getSelectionItemType(), receivedRealisationSelection.getSelectionItemType());

        assertEquals(requestCourseUnitSelection.getSelectionItemId(), receivedCourseUnitSelection.getSelectionItemId());
        assertEquals(requestCourseUnitSelection.getSelectionItemType(), receivedCourseUnitSelection.getSelectionItemType());

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(receivedRequest.getSelections());

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(), reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(), reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        RegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regRealisationSelection = reg.getSelectionsReplies().get(0);
        RegistrationSelection replyCourseUnitSelection = req.getSelections().get(0).getParent();
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regCourseUnitSelection = reg.getSelectionsReplies().get(0).getParent();

        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.REALISATION,
            regRealisationSelection.getSelectionItemType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED,
            regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.COURSE_UNIT,
            regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getSelectionItemStatus());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingRealisationRegistrationWithOutdatedSelection_shouldFail() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection groupSelection = DtoInitializer.getSelection(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetusryhmä 1", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionType.CHOOSE_ONE,
            Arrays.asList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Tentti", null, null), "TESTIRYHMA1", null),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingRealisationRegistrationWithOutdatedSelectionAndStatusAbortedByStudent_shouldSucceed() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection groupSelection = DtoInitializer.getSelection(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetusryhmä 1", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionType.CHOOSE_ONE,
            Arrays.asList(
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Tentti", null, null), "TESTIRYHMA1", null),
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                    new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
            Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(2));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
            realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.ABORTED_BY_STUDENT, courseUnitParent, List.of("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.singletonList(selection));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse resp = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
        assertTrue(StringUtils.isNotBlank(resp.getRegistrationRequestId()));
    }

    @Test
    public void testSendingAssessmentItemRealisationRegistration_shouldSuccess() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("AI1");

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId("CO1");
        completionOptionEntity.setAssessmentItems(Arrays.asList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithCompletionOptions("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null),
            Collections.singletonList(completionOptionEntity));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection completionOptionParent = DtoInitializerV8.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitParent);

        RegistrationSelection assessmentItemParent = DtoInitializerV8.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionParent);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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

        RegistrationSelection requestedSelection = req.getSelections().get(0);
        RegistrationSelection receivedRealisationSelection = receivedRequest.getSelections().get(0);
        assertEquals(requestedSelection.getSelectionItemId(), receivedRealisationSelection.getSelectionItemId());
        assertEquals(requestedSelection.getSelectionItemType(), receivedRealisationSelection.getSelectionItemType());

        RegistrationSelection assessmentItemSelection = requestedSelection.getParent();
        RegistrationSelection receivedAssessmentItemSelection = receivedRealisationSelection.getParent();
        assertEquals(assessmentItemSelection.getSelectionItemId(), receivedAssessmentItemSelection.getSelectionItemId());
        assertEquals(assessmentItemSelection.getSelectionItemType(), receivedAssessmentItemSelection.getSelectionItemType());

        RegistrationSelection completionOptionSelection = assessmentItemSelection.getParent();
        RegistrationSelection receivedCompletionOptionSelection = receivedAssessmentItemSelection.getParent();
        assertEquals(completionOptionSelection.getSelectionItemId(), receivedCompletionOptionSelection.getSelectionItemId());
        assertEquals(completionOptionSelection.getSelectionItemType(), receivedCompletionOptionSelection.getSelectionItemType());

        RegistrationSelection courseUnitSelection = completionOptionSelection.getParent();
        RegistrationSelection receivedCourseUnitSelection = receivedCompletionOptionSelection.getParent();
        assertEquals(courseUnitSelection.getSelectionItemId(), receivedCourseUnitSelection.getSelectionItemId());
        assertEquals(courseUnitSelection.getSelectionItemType(), receivedCourseUnitSelection.getSelectionItemType());

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(receivedRequest.getSelections());

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(), reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getStudyRightId(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        RegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regRealisationSelection = reg.getSelectionsReplies().get(0);
        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.REALISATION, regRealisationSelection.getSelectionItemType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED, regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        RegistrationSelection replyCompletionOptionSelection = replyRealisationSelection.getParent();
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regCompletionOptionSelection = regRealisationSelection.getParent();
        assertEquals(replyCompletionOptionSelection.getSelectionItemId(), regCompletionOptionSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.ASSESSMENT_ITEM,
            regCompletionOptionSelection.getSelectionItemType());
        assertNull(regCompletionOptionSelection.getSelectionItemStatus());
        assertNull(regCompletionOptionSelection.getSelectionItemStatusInfo());

        RegistrationSelection replyAssessmentItemSelection = replyCompletionOptionSelection.getParent();
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regAssessmentItemSelection = regCompletionOptionSelection.getParent();
        assertEquals(replyAssessmentItemSelection.getSelectionItemId(), regAssessmentItemSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.COMPLETION_OPTION,
            regAssessmentItemSelection.getSelectionItemType());
        assertNull(regAssessmentItemSelection.getSelectionItemStatus());
        assertNull(regAssessmentItemSelection.getSelectionItemStatusInfo());

        RegistrationSelection replyCourseUnitSelection = replyAssessmentItemSelection.getParent();
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regCourseUnitSelection = regAssessmentItemSelection.getParent();
        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.COURSE_UNIT,
            regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getSelectionItemStatus());
        assertNull(regCourseUnitSelection.getSelectionItemStatusInfo());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingRealisationWithGroupSelectionsRegistration_shouldSuccess() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection groupSelection = DtoInitializer.getSelection(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetusryhmä 1", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionType.CHOOSE_ONE,
                Arrays.asList(
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Tentti", null, null), "TESTIRYHMA1", null),
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(realisationEntity.getRealisationId(),
                RegistrationSelectionItemStatus.PENDING, courseUnitParent, Collections.singletonList("TESTIRYHMA1"));

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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

        RegistrationSelection requestSelection = req.getSelections().get(0);
        RegistrationSelection receivedSelection = receivedRequest.getSelections().get(0);
        assertTrue(receivedRequest.getSelections().stream().allMatch(s -> s.getSelectionItemStatus() == RegistrationSelectionItemStatus.PENDING));
        assertEquals(requestSelection.getSelectionItemId(), receivedSelection.getSelectionItemId());
        assertEquals(requestSelection.getSelectionItemType(), receivedSelection.getSelectionItemType());

        assertEquals(requestSelection.getParent().getSelectionItemId(), receivedSelection.getParent().getSelectionItemId());
        assertEquals(requestSelection.getParent().getSelectionItemType(), receivedSelection.getParent().getSelectionItemType());

        assertEquals(requestSelection.getSubGroupSelections().get(0), receivedSelection.getSubGroupSelections().get(0));

        // now the target university replies with a message "yes you are now registered"
        // accept registration for selected request
        for(RegistrationSelection sel : receivedRequest.getSelections()) {
            sel.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
            sel.setSelectionItemStatusInfo("Hyväksytty kurssille.");
        }

        RegistrationReplyRequest replyRequest = getRegistrationReplyTemplate();
        replyRequest.setRegistrationRequestId(receivedRequest.getRegistrationRequestId());
        replyRequest.setSelections(receivedRequest.getSelections());

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
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus.REGISTERED, reg.getStatus());

        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId(),
                reg.getStudent().getHomeStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(req.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getStudyRightId(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId());
        assertEquals(replyRequest.getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference(),
                reg.getStudent().getHostStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        RegistrationSelection replyRealisationSelection = req.getSelections().get(0);
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regRealisationSelection = reg.getSelectionsReplies().get(0);
        assertEquals(replyRealisationSelection.getSelectionItemId(), regRealisationSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.REALISATION, 
            regRealisationSelection.getSelectionItemType());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED, 
            regRealisationSelection.getSelectionItemStatus());
        assertEquals("Hyväksytty kurssille.", regRealisationSelection.getSelectionItemStatusInfo());

        RegistrationSelection replyCourseUnitSelection = req.getSelections().get(0).getParent();
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection regCourseUnitSelection = reg.getSelections().get(0).getParent();
        assertEquals(replyCourseUnitSelection.getSelectionItemId(), regCourseUnitSelection.getSelectionItemId());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType.COURSE_UNIT,
            regCourseUnitSelection.getSelectionItemType());
        assertNull(regCourseUnitSelection.getSelectionItemStatus());
        assertNull(regCourseUnitSelection.getParent());
    }

    @Test
    public void testSendingRealisationRegistrationWithNotEnoughGroupSelections_shouldFail() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection groupSelection = DtoInitializer.getSelection(new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetusryhmä 1", null, null),
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionType.CHOOSE_MANY,
                Arrays.asList(
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Tentti", null, null), "TESTIRYHMA1", null),
                        new fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue(null, null,
                            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opetus", null, null), "TESTIRYHMA2", null)));

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork), Collections.singletonList(groupSelection));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        realisationRepository.create(realisationEntity);

        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnitParent(courseUnitEntity.getStudyElementId());

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitParent, Collections.singletonList("TESTIRYHMA1"));

        // first test without selections. this should fail.
        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
        req.setSelections(Collections.emptyList());

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        // now test with a single selection. this should succeed, although SelectionType = CHOOSE_MANY (1 selection is considered "MANY" :)
        req.setSelections(Collections.singletonList(selection));
        responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, sendingOrganisation.getId());
        RegistrationResponse registrationResponse = (RegistrationResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(registrationResponse.getStatus() == Status.OK);
    }

    @Test
    public void testSendingAssessmentItemRealisationRegistration_shouldFailAssessmentItemNotInCourseUnit() throws JMSException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("AI1");

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId("CO1");
        completionOptionEntity.setAssessmentItems(Arrays.asList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntityWithCompletionOptions("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null), Collections.singletonList(completionOptionEntity));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference ref = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("T1", "T1", receivingOrganisation.getId(),
                Collections.singletonList(ref), Collections.singletonList(cooperationNetwork));
        realisationEntity.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntity.setEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        realisationRepository.create(realisationEntity);
        
        RegistrationSelection courseUnitParent = DtoInitializerV8.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionParent = DtoInitializerV8.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitParent);

        // "A2" is not found in courseunit.completionOptions.assessmentItems
        RegistrationSelection failingAssessmentItemParent = DtoInitializerV8.getRegistrationSelectionAssessmentItem(
                "AI2", completionOptionParent);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, failingAssessmentItemParent, null);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity validity =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity();
        validity.setStart(OffsetDateTime.now().minusDays(30));

        NetworkEntity wrongNetworkEntity = EntityInitializer.getNetworkEntity("DIFFERENT-NETWORKID-1",
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Väärä verkosto", null, null),
                testNetwork.getOrganisations(), validity, true);
        networkRepository.create(wrongNetworkEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
                true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
                Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.PENDING);

        RegistrationEntity reg = EntityInitializer.getRegistrationEntity(sendingOrganisation.getId(), receivingOrganisation.getId(),
            Collections.singletonList(selection), null, cooperationNetwork.getId());
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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatus() ==
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus.ACCEPTED));
        assertTrue(reg.getSelectionsReplies().stream().allMatch(s -> s.getSelectionItemStatusInfo().equals("Hyväksytty kurssille.")));

        Message statusMessageReceivedInOrganisation = jmsTemplate.receive(sendingOrganisation.getQueue());
        ForwardedRegistrationReplyRequest receivedStatusMessage = (ForwardedRegistrationReplyRequest) jmsTemplate.getMessageConverter().fromMessage(statusMessageReceivedInOrganisation);

        assertEquals(replyRequest.getRejectionReason().getValue("fi"), receivedStatusMessage.getRejectionReason().getValue("fi"));
        assertEquals(replyRequest.getRejectionReason().getValue("en"), receivedStatusMessage.getRejectionReason().getValue("en"));
        assertEquals(replyRequest.getRejectionReason().getValue("sv"), receivedStatusMessage.getRejectionReason().getValue("sv"));

    }

    @Test
    public void testSendingCourseUnitRegistrationRequestWithoutStudentOidOrStudentPersonId_shouldSucceed() throws JMSException, IOException {
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
        req.getStudent().setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

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
        fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(testNetwork.getId(), testNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("OJ1", "OJ1-CODE", receivingOrganisation.getId(),
            Collections.singletonList(cooperationNetwork), new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString("Opintojakso 1", null, null));

        courseUnitRepository.create(courseUnitEntity);

        RegistrationSelection selection = DtoInitializerV8.getRegistrationSelectionCourseUnit(
            courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest req = MessageTemplateInitializerV8.getCreateRegistrationRequestTemplate(sendingOrganisation.getId(), receivingOrganisation.getId(), testNetwork.getId());
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
