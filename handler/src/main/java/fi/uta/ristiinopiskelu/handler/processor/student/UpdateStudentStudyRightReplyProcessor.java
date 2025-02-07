package fi.uta.ristiinopiskelu.handler.processor.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.StudentMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.student.ForwardedUpdateStudentStudyRightReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.student.UpdateStudentStudyRightReplyRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UpdateStudentStudyRightReplyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudentStudyRightReplyProcessor.class);

    private StudentService studentService;
    private OrganisationService organisationService;
    private ObjectMapper objectMapper;
    private JmsMessageForwarder jmsMessageForwarder;
    private ModelMapper modelMapper;

    @Autowired
    public UpdateStudentStudyRightReplyProcessor(StudentService studentService, OrganisationService organisationService,
                                                 ObjectMapper objectMapper, JmsMessageForwarder jmsMessageForwarder, ModelMapper modelMapper) {
        this.studentService = studentService;
        this.organisationService = organisationService;
        this.objectMapper = objectMapper;
        this.jmsMessageForwarder = jmsMessageForwarder;
        this.modelMapper = modelMapper;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Replies to a student study right update request",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Replies to a student study right update request"
            ),
            payloadType = UpdateStudentStudyRightReplyRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded student study right update reply",
            servers = {"production", "staging"},
            payloadType = ForwardedUpdateStudentStudyRightReplyRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        UpdateStudentStudyRightReplyRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), UpdateStudentStudyRightReplyRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);
        StudentEntity entity = studentService.findById(request.getStudentRequestId())
                .orElseThrow(() -> new EntityNotFoundException(StudentEntity.class, request.getStudentRequestId()));

        OrganisationEntity homeOrganisation = organisationService.findById(entity.getHomeOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, entity.getHomeOrganisationTkCode()));

        ForwardedUpdateStudentStudyRightReplyRequest forwardedRequest = modelMapper.map(request, ForwardedUpdateStudentStudyRightReplyRequest.class);
        forwardedRequest.setSendingOrganisationTkCode(organisationId);

        jmsMessageForwarder.forwardRequestToOrganisation(forwardedRequest.getStudentRequestId(), forwardedRequest,
                MessageType.FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST, correlationId, homeOrganisation,
                Collections.singletonMap("studentRequestId", request.getStudentRequestId()));

        exchange.setMessage(new StudentMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId, request.getStudentRequestId(),
                new DefaultResponse(Status.OK, "Cancel student status -message reply processed successfully and forwarded to student's home organisation.")));
    }
}
