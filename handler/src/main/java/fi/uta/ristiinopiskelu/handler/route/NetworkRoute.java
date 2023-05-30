package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.network.CreateNetworkProcessor;
import fi.uta.ristiinopiskelu.handler.processor.network.UpdateNetworkProcessor;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class NetworkRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(NetworkRoute.class);

    @Value("${general.camel.route.network.create}")
    private String createNetworkRoute;

    @Value("${general.camel.route.network.update}")
    private String updateNetworkRoute;

    @Value("${general.camel.route.network.max-threads:1}")
    private int maxThreads;

    @Autowired
    private CreateNetworkProcessor createNetworkProcessor;

    @Autowired
    private UpdateNetworkProcessor updateNetworkProcessor;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.NETWORK.getQueueName();
    }

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(createNetworkRoute,
                        "createNetworkRequest.json",
                        MessageType.CREATE_NETWORK_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        createNetworkProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateNetworkRoute,
                        "updateNetworkRequest.json",
                        MessageType.UPDATE_NETWORK_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        updateNetworkProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()));
    }
}
