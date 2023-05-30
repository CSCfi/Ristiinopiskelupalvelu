package fi.uta.ristiinopiskelu.handler.processor.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.network.CreateNetworkValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateNetworkProcessor extends AbstractNetworkProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CreateNetworkProcessor.class);

    private NetworkService networkService;
    private ObjectMapper objectMapper;
    private CreateNetworkValidator validator;

    @Autowired
    public CreateNetworkProcessor(NetworkService networkService, OrganisationService organisationService, CreateNetworkValidator validator,
                                  ObjectMapper objectMapper, JmsMessageForwarder jmsMessageForwarder) {
        super(organisationService, jmsMessageForwarder);
        this.networkService = networkService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        CreateNetworkRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateNetworkRequest.class);
        validator.validateRequest(request, null);

        NetworkEntity createdNetwork = networkService.create(NetworkEntity.fromDto(request.getNetwork()));

        if(createdNetwork.isPublished()) {
            sendNetworkCreatedNotification(createdNetwork);
        }

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
            new DefaultResponse(Status.OK, "Network created successfully")));
    }
}
