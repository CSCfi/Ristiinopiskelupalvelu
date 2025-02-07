package fi.uta.ristiinopiskelu.handler.processor.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.UpdateStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.NoForwardingOrganisationsValidationException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.service.result.EntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudentMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.StudentResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.apache.commons.collections4.MapUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UpdateStudentProcessor extends AbstractStudentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudentProcessor.class);

    private StudentService studentService;
    private RegistrationService registrationService;
    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;

    @Autowired
    public UpdateStudentProcessor(StudentService studentService, RegistrationService registrationService,
                                  ObjectMapper objectMapper, ModelMapper modelMapper, JmsMessageForwarder jmsMessageForwarder) {
        super(jmsMessageForwarder);
        this.studentService = studentService;
        this.registrationService = registrationService;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Updates a student",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Updates a student"
            ),
            payloadType = UpdateStudentRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded student update request",
            servers = {"production", "staging"},
            payloadType = ForwardedUpdateStudentRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        UpdateStudentRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), UpdateStudentRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);
        Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations =
                registrationService.findAllStudentRegistrationsPerOrganisation(organisationId, request.getPersonId(),
                        request.getOid(), Pageable.unpaged());

        // If no registrations / organisations are found return failure message
        if (MapUtils.isEmpty(organisationsAndRegistrations)) {
            throw new NoForwardingOrganisationsValidationException(
                    "Stopped handling update student -message. Did not find any registrations for given student." +
                            " Message would not be forwarded to anyone so it is redundant.");
        }

        StudentEntity student = modelMapper.map(request, StudentEntity.class);
        student.setId(null);
        student.setTimestamp(OffsetDateTime.now());
        student.setHomeOrganisationTkCode(organisationId);
        student.setMessageType(StudentMessageType.UPDATE_STUDENT);

        List<? extends EntityModificationResult<?>> modificationResults = studentService.create(student);
        student = (StudentEntity) modificationResults.get(0).getCurrent();

        ForwardedUpdateStudentRequest forwardedRequest = modelMapper.map(request, ForwardedUpdateStudentRequest.class);
        forwardedRequest.setStudentRequestId(student.getId());

        // send to correct organisations and add host study rights
        super.forwardMessage(exchange, MessageType.FORWARDED_UPDATE_STUDENT_REQUEST, organisationsAndRegistrations, forwardedRequest,
                Collections.singletonMap("studentRequestId", forwardedRequest.getStudentRequestId()));

        exchange.setMessage(new StudentMessage(exchange, MessageType.STUDENT_RESPONSE, correlationId, forwardedRequest.getStudentRequestId(),
                new StudentResponse(Status.OK, "Update student request processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getStudentRequestId())));
    }
}
