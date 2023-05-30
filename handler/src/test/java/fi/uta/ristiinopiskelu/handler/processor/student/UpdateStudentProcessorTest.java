package fi.uta.ristiinopiskelu.handler.processor.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentRequest;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateStudentProcessorTest {

    @MockBean
    private StudentService studentService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private ModelMapper modelMapper;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    private UpdateStudentProcessor updateStudentProcessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper();
        updateStudentProcessor = mock(UpdateStudentProcessor.class, withSettings()
            .useConstructor(studentService, registrationService, objectMapper, new ModelMapper(), jmsMessageForwarder)
            .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testProcess_shouldSuccess() throws Exception {
        OrganisationEntity org1 = new OrganisationEntity();
        org1.setId("ORG1");

        OrganisationEntity org2 = new OrganisationEntity();
        org2.setId("ORG2");

        List<RegistrationEntity> registrationEntitiesForOrg1 = new ArrayList<>();
        createRegistrationsForOrganisation(org1, registrationEntitiesForOrg1, 0, 1);

        List<RegistrationEntity> registrationEntitiesForOrg2 = new ArrayList<>();
        createRegistrationsForOrganisation(org2, registrationEntitiesForOrg2, 2, 3);

        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations =
            new HashMap<>() {
                {
                    put(org1, registrationEntitiesForOrg1);
                    put(org2, registrationEntitiesForOrg2);
                }
            };

        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setId("STUDENT-ID1");

        when(registrationService.findAllStudentRegistrationsPerOrganisation(eq("TUNI"), any(), any(), any())).thenReturn(organisationsAndRegistrations);
        when(studentService.create(any())).thenReturn(studentEntity);
        doNothing().when(jmsMessageForwarder).forwardRequestToOrganisation(anyString(), any(), eq(MessageType.FORWARDED_UPDATE_STUDENT_REQUEST), anyString(), any(OrganisationEntity.class));

        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setOid("123123.141.1231");
        request.setFirstNames("Jaakko");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");

        updateStudentProcessor.process(exchange);

        verify(registrationService, times(1)).findAllStudentRegistrationsPerOrganisation(eq("TUNI"), eq(request.getPersonId()), eq(request.getOid()), eq(Pageable.unpaged()));
        verify(studentService, times(1)).create(any());
        verify(jmsMessageForwarder, times(2)).forwardRequestToOrganisation(anyString(), any(), any(), any(), any(), any());
    }


    @Test
    public void testProcess_messageForwardingFails_shouldThrowMessageForwardingFailedException() throws Exception {
        OrganisationEntity org1 = new OrganisationEntity();
        org1.setId("ORG1");

        OrganisationEntity org2 = new OrganisationEntity();
        org2.setId("ORG2");

        List<RegistrationEntity> registrationEntitiesForOrg1 = new ArrayList<>();
        createRegistrationsForOrganisation(org1, registrationEntitiesForOrg1, 0, 1);

        List<RegistrationEntity> registrationEntitiesForOrg2 = new ArrayList<>();
        createRegistrationsForOrganisation(org2, registrationEntitiesForOrg2, 2, 3);

        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations =
            new HashMap<>() {
                {
                    put(org1, registrationEntitiesForOrg1);
                    put(org2, registrationEntitiesForOrg2);
                }
            };

        StudentEntity studentEntity = new StudentEntity();
        studentEntity.setId("STUDENT-ID1");

        when(registrationService.findAllStudentRegistrationsPerOrganisation(eq("TUNI"), any(), any(), any())).thenReturn(organisationsAndRegistrations);
        when(studentService.create(any())).thenReturn(studentEntity);
        doNothing().when(jmsMessageForwarder).forwardRequestToOrganisation(anyString(), any(),
            eq(MessageType.FORWARDED_UPDATE_STUDENT_REQUEST), any(), eq(org1), any());
        doThrow(new MessageForwardingFailedException("FAILURE")).when(jmsMessageForwarder)
            .forwardRequestToOrganisation(anyString(), any(), eq(MessageType.FORWARDED_UPDATE_STUDENT_REQUEST), any(), eq(org2), any());

        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setOid("123123.141.1231");
        request.setFirstNames("Jaakko");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");

        Exception e = assertThrows(MessageForwardingFailedException.class, () -> updateStudentProcessor.process(exchange));
        assertTrue(e.getMessage().contains("Forwarding failed to organisations: ORG2"));
        assertTrue(e.getMessage().contains("Forwarding was successful to organisations: ORG1"));

        verify(registrationService, times(1)).findAllStudentRegistrationsPerOrganisation(eq("TUNI"), eq(request.getPersonId()), eq(request.getOid()), eq(Pageable.unpaged()));
        verify(studentService, times(1)).create(any());
        verify(jmsMessageForwarder, times(2))
            .forwardRequestToOrganisation(anyString(), any(), any(), any(), any(), any());
    }

    private void createRegistrationsForOrganisation(OrganisationEntity org, List<RegistrationEntity> registrationEntitieForOrg, int from, int to) {
        for(int i=from; i < to; i++) {
            StudyRightIdentifier hostStudyRightIdentifiers = new StudyRightIdentifier();
            hostStudyRightIdentifiers.setStudyRightId("OIK" + i);
            hostStudyRightIdentifiers.setOrganisationTkCodeReference(org.getId());

            StudyRight hostStudyRight = new StudyRight();
            hostStudyRight.setIdentifiers(hostStudyRightIdentifiers);

            ExtendedStudent student = new ExtendedStudent();
            student.setHostStudyRight(hostStudyRight);

            RegistrationEntity regForOrg = new RegistrationEntity();
            regForOrg.setStudent(student);

            registrationEntitieForOrg.add(regForOrg);
        }
    }
}
