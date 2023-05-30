package fi.uta.ristiinopiskelu.handler.processor.realisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.realisation.UpdateRealisationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UpdateRealisationProcessor extends AbstractCompositeIdentifiedEntityProcessor<RealisationEntity> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateRealisationProcessor.class);

    private RealisationService realisationService;
    private UpdateRealisationValidator realisationValidator;
    private ObjectMapper objectMapper;

    @Autowired
    public UpdateRealisationProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      RealisationService realisationService,
                                      UpdateRealisationValidator realisationValidator,
                                      ObjectMapper objectMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.realisationService = realisationService;
        this.realisationValidator = realisationValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = objectMapper.readTree(exchange.getIn().getBody(String.class));

        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        RealisationEntity original = realisationValidator.validateJson(requestJsonNodeTree, organisationId);
        RealisationEntity updated = realisationService.update(requestJsonNodeTree.get("realisation"), organisationId);

        // send notifications based on the original entity information
        super.notifyNetworkMembers(organisationId, MessageType.REALISATION_UPDATED_NOTIFICATION,
            new DefaultCompositeIdentifiedEntityModificationResult(null, Collections.singletonList(original), null));

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
            new DefaultResponse(Status.OK, "Realisation with id " + updated.getRealisationId() + " updated successfully")));
    }
}
