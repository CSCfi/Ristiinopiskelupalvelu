package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.realisation.CreateRealisationProcessor;
import fi.uta.ristiinopiskelu.handler.processor.realisation.DeleteRealisationProcessor;
import fi.uta.ristiinopiskelu.handler.processor.realisation.UpdateRealisationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RealisationRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(RealisationRoute.class);

    @Value("${general.camel.route.realisation.create}")
    private String createRealisationRoute;

    @Value("${general.camel.route.realisation.update}")
    private String updateRealisationRoute;

    @Value("${general.camel.route.realisation.delete}")
    private String deleteRealisationRoute;

    @Value("${general.camel.route.realisation.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private CreateRealisationProcessor createRealisationProcessor;

    @Autowired
    private UpdateRealisationProcessor updateRealisationProcessor;

    @Autowired
    private DeleteRealisationProcessor deleteRealisationProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(createRealisationRoute,
                        "createRealisationRequest.json",
                        MessageType.CREATE_REALISATION_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        createRealisationProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(updateRealisationRoute,
                        "updateRealisationRequest.json",
                        MessageType.UPDATE_REALISATION_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        updateRealisationProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(deleteRealisationRoute,
                        "deleteRealisationRequest.json",
                        MessageType.DELETE_REALISATION_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                        deleteRealisationProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()));
    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.REALISATION.getQueueName();
    }
}
