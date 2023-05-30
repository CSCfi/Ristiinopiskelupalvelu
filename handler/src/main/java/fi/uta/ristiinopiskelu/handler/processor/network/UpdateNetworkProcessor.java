package fi.uta.ristiinopiskelu.handler.processor.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.network.UpdateNetworkValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UpdateNetworkProcessor extends AbstractNetworkProcessor {
    private static final Logger logger = LoggerFactory.getLogger(UpdateNetworkProcessor.class);

    private NetworkService networkService;
    private ObjectMapper objectMapper;
    private UpdateNetworkValidator validator;
    
    @Autowired
    public UpdateNetworkProcessor(NetworkService networkService, OrganisationService organisationService, UpdateNetworkValidator validator,
                                  ObjectMapper objectMapper, JmsMessageForwarder jmsMessageForwarder) {
        super(organisationService, jmsMessageForwarder);
        this.networkService = networkService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = objectMapper.readTree(exchange.getIn().getBody(String.class));
        String messageId = exchange.getIn().getHeader("JMSMessageID", String.class);
        NetworkEntity originalNetwork = validator.validateJson(requestJsonNodeTree, null);

        logger.info("Handling UpdateNetworkRequest for network " + requestJsonNodeTree.get("network").get("id").asText() +
            " (original: " + originalNetwork.getId() + ", published: " + originalNetwork.isPublished() + ", organisations: " +
            originalNetwork.getOrganisations().stream().map(o -> o.getOrganisationTkCode()).collect(Collectors.joining(", ")) + ")");

        NetworkEntity updatedNetwork = networkService.update(requestJsonNodeTree.get("network"));

        logger.info("Updated network " + updatedNetwork.getId() + " (published: " + updatedNetwork.isPublished() + ", organisations: " +
            updatedNetwork.getOrganisations().stream().map(o -> o.getOrganisationTkCode()).collect(Collectors.joining(", ")) + ")");

        if(!originalNetwork.isPublished() && updatedNetwork.isPublished()) {
            logger.info("Network was previously unpublished, sending create notification");
            sendNetworkCreatedNotification(updatedNetwork);
        } else if(originalNetwork.isPublished()) {
            logger.info("Network was previously published, sending update notification");
            sendNetworkUpdatedNotification(messageId, updatedNetwork);
        }

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
            new DefaultResponse(Status.OK, "Network updated successfully")));
    }
}
