package fi.uta.ristiinopiskelu.handler.processor.registration;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.result.EntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.registration.CreateRegistrationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RegistrationMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedCreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class CreateRegistrationProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(CreateRegistrationProcessor.class);

    private RegistrationService registrationService;
    private OrganisationService organisationService;
    private JsonMapper jsonMapper;
    private CreateRegistrationValidator validator;
    private JmsMessageForwarder messageForwarder;

    @Autowired
    public CreateRegistrationProcessor(RegistrationService registrationService,
                                       OrganisationService organisationService,
                                       CreateRegistrationValidator validator,
                                       JsonMapper jsonMapper,
                                       JmsMessageForwarder messageForwarder) {
        this.registrationService = registrationService;
        this.organisationService = organisationService;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
        this.messageForwarder = messageForwarder;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        CreateRegistrationRequest request = jsonMapper.readValue(exchange.getIn().getBody(String.class), CreateRegistrationRequest.class);
        logger.info("Handling registration request");

        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader(MessageHeader.JMS_MESSAGEID, String.class);
        Long messageTimestamp = exchange.getIn().getHeader(MessageHeader.MESSAGE_TIMESTAMP, Long.class);
        Long messageSequence = exchange.getIn().getHeader(MessageHeader.MESSAGE_SEQUENCE, Long.class);

        validator.validateRequest(request, organisationId);

        OrganisationEntity organisation = organisationService.findById(request.getReceivingOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, request.getReceivingOrganisationTkCode()));

        // Persist the request. RegistrationRequest & RegistrationReply map nicely to entity :)
        RegistrationEntity registrationEntity = CreateRegistrationRequest.toRegistrationEntity(request);
        registrationEntity.setReceivingDateTime(OffsetDateTime.now());
        registrationEntity.setReceivingTimestamp(messageTimestamp);
        registrationEntity.setReceivingSequence(messageSequence);

        List<? extends EntityModificationResult<?>> modificationResults = registrationService.create(registrationEntity);
        registrationEntity = (RegistrationEntity) modificationResults.get(0).getCurrent();

        // Add generated registration id for registration
        ForwardedCreateRegistrationRequest forwardedRequest = ForwardedCreateRegistrationRequest.from(request);
        forwardedRequest.setRegistrationRequestId(registrationEntity.getId());

        // send to correct organisations
        this.messageForwarder.forwardRequestToOrganisation(registrationEntity.getId(), forwardedRequest,
                MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, correlationId, organisation,
                Collections.singletonMap("registrationRequestId", forwardedRequest.getRegistrationRequestId()));

        registrationEntity.setSendDateTime(OffsetDateTime.now());
        registrationService.update(registrationEntity);

        exchange.setMessage(new RegistrationMessage(exchange, MessageType.REGISTRATION_RESPONSE, correlationId, forwardedRequest.getRegistrationRequestId(),
                new RegistrationResponse(Status.OK, "Registration requests processed successfully and forwarded to recipient organisations",
                        forwardedRequest.getRegistrationRequestId())));
    }
}
