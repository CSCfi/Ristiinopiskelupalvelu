package fi.uta.ristiinopiskelu.handler.processor.realisation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.realisation.UpdateRealisationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.UpdateRealisationRequest;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateRealisationProcessor extends AbstractCompositeIdentifiedEntityProcessor<RealisationEntity> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateRealisationProcessor.class);

    private RealisationService realisationService;
    private UpdateRealisationValidator realisationValidator;
    private JsonMapper jsonMapper;

    @Autowired
    public UpdateRealisationProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      RealisationService realisationService,
                                      UpdateRealisationValidator realisationValidator,
                                      JsonMapper jsonMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.realisationService = realisationService;
        this.realisationValidator = realisationValidator;
        this.jsonMapper = jsonMapper;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = jsonMapper.readTree(exchange.getIn().getBody(String.class));

        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        RealisationEntity original = realisationValidator.validateJson(requestJsonNodeTree, organisationId);
        List<CompositeIdentifiedEntityModificationResult> modificationResults = realisationService.update(requestJsonNodeTree.get("realisation"), organisationId);

        super.notifyNetworkMembers(organisationId, MessageType.REALISATION_UPDATED_NOTIFICATION, modificationResults);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Realisation with id " + original.getRealisationId() + " updated successfully")));
    }
}
