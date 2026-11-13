package fi.uta.ristiinopiskelu.handler.processor.student;

import tools.jackson.databind.json.JsonMapper;
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
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UpdateStudentStudyRightReplyProcessor implements Processor {

    private StudentService studentService;
    private OrganisationService organisationService;
    private JsonMapper jsonMapper;
    private JmsMessageForwarder jmsMessageForwarder;

    @Autowired
    public UpdateStudentStudyRightReplyProcessor(StudentService studentService, OrganisationService organisationService,
                                                 JsonMapper jsonMapper, JmsMessageForwarder jmsMessageForwarder) {
        this.studentService = studentService;
        this.organisationService = organisationService;
        this.jsonMapper = jsonMapper;
        this.jmsMessageForwarder = jmsMessageForwarder;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        UpdateStudentStudyRightReplyRequest request = jsonMapper.readValue(exchange.getIn().getBody(String.class), UpdateStudentStudyRightReplyRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader(MessageHeader.JMS_MESSAGEID, String.class);
        StudentEntity entity = studentService.findById(request.getStudentRequestId())
                .orElseThrow(() -> new EntityNotFoundException(StudentEntity.class, request.getStudentRequestId()));

        OrganisationEntity homeOrganisation = organisationService.findById(entity.getHomeOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, entity.getHomeOrganisationTkCode()));

        ForwardedUpdateStudentStudyRightReplyRequest forwardedRequest = ForwardedUpdateStudentStudyRightReplyRequest.from(request);
        forwardedRequest.setSendingOrganisationTkCode(organisationId);

        jmsMessageForwarder.forwardRequestToOrganisation(forwardedRequest.getStudentRequestId(), forwardedRequest,
                MessageType.FORWARDED_UPDATE_STUDENT_STUDYRIGHT_REPLY_REQUEST, correlationId, homeOrganisation,
                Collections.singletonMap("studentRequestId", request.getStudentRequestId()));

        exchange.setMessage(new StudentMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId, request.getStudentRequestId(),
                new DefaultResponse(Status.OK, "Cancel student status -message reply processed successfully and forwarded to student's home organisation.")));
    }
}
