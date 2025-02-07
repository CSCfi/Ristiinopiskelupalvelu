package fi.uta.ristiinopiskelu.handler.processor.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.validator.registration.RegistrationStatusValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RegistrationMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedRegistrationReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationReplyRequest;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RegistrationReplyProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationReplyProcessor.class);

    private RegistrationStatusValidator validator;
    private RegistrationService registrationService;
    private OrganisationService organisationService;
    private ObjectMapper objectMapper;
    private JmsMessageForwarder messageForwarder;

    @Autowired
    public RegistrationReplyProcessor(RegistrationService registrationService, OrganisationService organisationService,
                                      RegistrationStatusValidator validator, ObjectMapper objectMapper,
                                      JmsMessageForwarder messageForwarder) {
        this.validator = validator;
        this.registrationService = registrationService;
        this.organisationService = organisationService;
        this.objectMapper = objectMapper;
        this.messageForwarder = messageForwarder;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Replies to a registration request",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Replies to a registration request"
            ),
            payloadType = RegistrationReplyRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Forwarded registration reply",
            servers = {"production", "staging"},
            payloadType = ForwardedRegistrationReplyRequest.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        RegistrationReplyRequest req = objectMapper.readValue(exchange.getIn().getBody(String.class), RegistrationReplyRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);

        logger.info("Handling registration reply request {}", req.toString());

        validator.validateRequest(req, organisationId);

        RegistrationEntity registrationEntity = registrationService.findById(req.getRegistrationRequestId())
                .orElseThrow(() -> new EntityNotFoundException(RegistrationEntity.class, req.getRegistrationRequestId()));

        OrganisationEntity organisation = organisationService.findById(registrationEntity.getSendingOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, registrationEntity.getSendingOrganisationTkCode()));

        // Persist the reply.
        if (req.getStatus() == RegistrationStatus.RECEIVED) {
            registrationEntity.setStatus(req.getStatus());
        } else {
            registrationEntity.setSelectionsReplies(req.getSelections());
            registrationEntity.setStatus(req.getStatus());
            registrationEntity.setStatusInfo(req.getStatusInfo());
            registrationEntity.getStudent().setHostStudyRight(req.getHostStudyRight());
            registrationEntity.getStudent().setHostEppn(req.getHostStudentEppn());
            registrationEntity.getStudent().setHostStudentNumber(req.getHostStudentNumber());
            registrationEntity.setRejectionReason(req.getRejectionReason());
        }
        registrationService.update(registrationEntity);

        // send to correct organisations
        this.messageForwarder.forwardRequestToOrganisation(registrationEntity.getId(), new ForwardedRegistrationReplyRequest(registrationEntity),
                MessageType.FORWARDED_REGISTRATION_REPLY_REQUEST, correlationId, organisation,
                Collections.singletonMap("registrationRequestId", req.getRegistrationRequestId()));

        exchange.setMessage(new RegistrationMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId, req.getRegistrationRequestId(),
                new DefaultResponse(Status.OK, "Registration replies processed successfully and forwarded to recipient organisations")));
    }
}
