package fi.uta.ristiinopiskelu.handler.processor.registration;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRightEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationReplySelectionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationStatusEntity;
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
    private JsonMapper jsonMapper;
    private JmsMessageForwarder messageForwarder;

    @Autowired
    public RegistrationReplyProcessor(RegistrationService registrationService, OrganisationService organisationService,
                                      RegistrationStatusValidator validator, JsonMapper jsonMapper,
                                      JmsMessageForwarder messageForwarder) {
        this.validator = validator;
        this.registrationService = registrationService;
        this.organisationService = organisationService;
        this.jsonMapper = jsonMapper;
        this.messageForwarder = messageForwarder;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        RegistrationReplyRequest req = jsonMapper.readValue(exchange.getIn().getBody(String.class), RegistrationReplyRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader(MessageHeader.JMS_MESSAGEID, String.class);

        logger.info("Handling registration reply request {}", req.toString());

        validator.validateRequest(req, organisationId);

        RegistrationEntity registrationEntity = registrationService.findById(req.getRegistrationRequestId())
                .orElseThrow(() -> new EntityNotFoundException(RegistrationEntity.class, req.getRegistrationRequestId()));

        OrganisationEntity organisation = organisationService.findById(registrationEntity.getSendingOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, registrationEntity.getSendingOrganisationTkCode()));

        // Persist the reply.
        if (req.getStatus() == RegistrationStatus.RECEIVED) {
            registrationEntity.setStatus(RegistrationStatusEntity.from(req.getStatus()));
        } else {
            registrationEntity.setSelectionsReplies(RegistrationReplySelectionEntity.from(req.getSelections()));
            registrationEntity.setStatus(RegistrationStatusEntity.from(req.getStatus()));
            registrationEntity.setStatusInfo(req.getStatusInfo());
            registrationEntity.getStudent().setHostStudyRight(StudyRightEntity.fromStudyRight(req.getHostStudyRight()));
            registrationEntity.getStudent().setHostStudentNumber(req.getHostStudentNumber());
            registrationEntity.setRejectionReason(LocalisedStringEntity.from(req.getRejectionReason()));
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
