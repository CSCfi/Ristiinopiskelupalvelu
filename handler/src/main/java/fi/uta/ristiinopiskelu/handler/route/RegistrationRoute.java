package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.DefaultErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.JsonValidationErrorProcessor;
import fi.uta.ristiinopiskelu.handler.processor.registration.CreateRegistrationProcessor;
import fi.uta.ristiinopiskelu.handler.processor.registration.RegistrationReplyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RegistrationRoute extends AbstractRoute {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationRoute.class);

    @Value("${general.camel.route.registration.request}")
    private String registrationRequestRoute;

    @Value("${general.camel.route.registration.reply}")
    private String registrationReplyRoute;

    @Value("${general.camel.route.registration.max-threads:1}")
    private int maxThreads;

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private CreateRegistrationProcessor createRegistrationProcessor;

    @Autowired
    private RegistrationReplyProcessor registrationReplyProcessor;

    @Override
    protected String getQueueUri() {
        return MessageGroup.REGISTRATION.getQueueName();
    }

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return Arrays.asList(
                new RouteConfiguration(
                        registrationRequestRoute,
                        "createRegistrationRequest.json",
                        MessageType.CREATE_REGISTRATION_REQUEST,
                        MessageType.REGISTRATION_RESPONSE,
                    createRegistrationProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads()),

                new RouteConfiguration(
                    registrationReplyRoute,
                        "registrationReplyRequest.json",
                        MessageType.REGISTRATION_REPLY_REQUEST,
                        MessageType.DEFAULT_RESPONSE,
                    registrationReplyProcessor,
                        new DefaultErrorProcessor(),
                        new JsonValidationErrorProcessor(),
                       true,
                        getMinThreads(), getMaxThreads())
        );
    }
}
