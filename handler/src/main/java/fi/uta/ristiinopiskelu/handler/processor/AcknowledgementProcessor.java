package fi.uta.ristiinopiskelu.handler.processor;


import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.acknowledgement.AcknowledgementValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AcknowledgementProcessor implements Processor {

    private ObjectMapper objectMapper;
    private OrganisationService organisationService;
    private AcknowledgementValidator validator;
    private JmsMessageForwarder messageForwarder;

    @Autowired
    public AcknowledgementProcessor(OrganisationService organisationService,
                                    AcknowledgementValidator acknowledgementValidator,
                                    ObjectMapper objectMapper,
                                    JmsMessageForwarder messageForwarder){
        this.organisationService = organisationService;
        this.validator = acknowledgementValidator;
        this.objectMapper = objectMapper;
        this.messageForwarder = messageForwarder;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Acknowledgement ack = objectMapper.readValue(exchange.getIn().getBody(String.class), Acknowledgement.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String correlationId = exchange.getIn().getHeader("ripaCorrelationID", String.class);

        validator.validateRequest(ack, organisationId);

        OrganisationEntity organisation = organisationService.findById(ack.getReceivingOrganisationTkCode())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, ack.getReceivingOrganisationTkCode()));


        this.messageForwarder.forwardRequestToOrganisation(ack, MessageType.ACKNOWLEDGEMENT, correlationId, organisation);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId,
            new DefaultResponse(Status.OK, "Acknowledgement passed on successfully")));
    }
}
