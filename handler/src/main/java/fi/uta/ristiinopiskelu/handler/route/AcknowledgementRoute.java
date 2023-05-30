package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.handler.processor.AcknowledgementProcessor;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
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
public class AcknowledgementRoute extends AbstractRoute {

    private static final Logger logger  = LoggerFactory.getLogger(AcknowledgementRoute.class);

    @Value("${general.camel.route.acknowledgement.send}")
    private String acknowledgementRoute;


    @Autowired
    private AcknowledgementProcessor acknowledgementProcessor;

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
            new RouteConfiguration(
                acknowledgementRoute,
                "acknowledgement.json",
                MessageType.ACKNOWLEDGEMENT,
                MessageType.DEFAULT_RESPONSE,
                acknowledgementProcessor,
                new DefaultErrorProcessor(),
                new JsonValidationErrorProcessor(),
                false)
        );

    }

    @Override
    protected String getQueueUri() {
        return MessageGroup.ACKNOWLEDGEMENT.getQueueName();
    }

}
