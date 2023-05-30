package fi.uta.ristiinopiskelu.handler.processor.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.UpdateStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.NoForwardingOrganisationsValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.OrganizingOrganisationMismatchValidationException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudentMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentStudyRightRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.StudentResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentStudyRightRequest;
import org.apache.camel.Exchange;
import org.apache.commons.collections4.MapUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UpdateStudentStudyRightProcessor extends AbstractStudentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudentStudyRightProcessor.class);

    private RegistrationService registrationService;
    private StudentService studentService;
    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;

    @Autowired
    public UpdateStudentStudyRightProcessor(StudentService studentService, RegistrationService registrationService,
                                             ObjectMapper objectMapper, ModelMapper modelMapper, JmsMessageForwarder jmsMessageForwarder) {
        super(jmsMessageForwarder);
        this.registrationService = registrationService;
        this.studentService = studentService;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        UpdateStudentStudyRightRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), UpdateStudentStudyRightRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);

        if(!organisationId.equals(request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference())) {
            throw new OrganizingOrganisationMismatchValidationException("Organisation sending the request must match home " +
                "study right organizing organisation");
        }

        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations = registrationService.findAllRegistrationsWithValidStudyRightPerOrganisation(
                request.getHomeStudyRight().getIdentifiers().getStudyRightId(), request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference());

        // If no registrations / organisations are found return failure message
        if(MapUtils.isEmpty(organisationsAndRegistrations)) {
            throw new NoForwardingOrganisationsValidationException(
                "Stopped handling update student study right-message. No registrations found with " +
                "[studyRightId: " + request.getHomeStudyRight().getIdentifiers().getStudyRightId() +
                " organisationId: " + request.getHomeStudyRight().getIdentifiers().getOrganisationTkCodeReference() +
                " Message would not be forwarded to anyone so it is redundant.");
        }

        StudentEntity student = modelMapper.map(request, StudentEntity.class);
        student.setId(null);
        student.setHomeOrganisationTkCode(organisationId);
        student.setTimestamp(OffsetDateTime.now());
        student.setMessageType(StudentMessageType.UPDATE_STUDENT_STUDYRIGHT);
        student.setStatuses(organisationsAndRegistrations.keySet().stream()
            .map(o -> new UpdateStatus(o.getId(), StudentStatus.PENDING)).collect(Collectors.toList()));
        student = studentService.create(student);

        ForwardedUpdateStudentStudyRightRequest forwardedRequest = modelMapper.map(request, ForwardedUpdateStudentStudyRightRequest.class);
        forwardedRequest.setStudentRequestId(student.getId());

        // send to correct organisations and add host study rights
        super.forwardMessage(exchange, MessageType.FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REQUEST, organisationsAndRegistrations,
            forwardedRequest, Collections.singletonMap("studentRequestId", forwardedRequest.getStudentRequestId()));
            
        exchange.setMessage(new StudentMessage(exchange, MessageType.STUDENT_RESPONSE, correlationId, forwardedRequest.getStudentRequestId(),
                new StudentResponse(Status.OK, "Cancel student request processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getStudentRequestId())));
    }
}
