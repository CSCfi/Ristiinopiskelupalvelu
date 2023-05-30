package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.handler.processor.AuthenticationFailedProcessor;
import fi.uta.ristiinopiskelu.handler.processor.AuthenticationProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ThreadsDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BaseRoute extends AbstractRoute {

    @Value("${general.activemq.queue}")
    private String queueUri;

    @Value("${general.camel.route.base.max-threads:1}")
    private int maxThreads;

    @Override
    protected int getMaxThreads() {
        return maxThreads;
    }

    @Autowired
    private AuthenticationProcessor authenticationProcessor;

    private MessageType failMessageResponseType = MessageType.AUTHENTICATION_FAILED_RESPONSE;

    @Override
    protected String getQueueUri() {
        return this.queueUri;
    }

    @Override
    public void configure() throws Exception {
        ThreadsDefinition def = from(getQueueUri()).threads(getMinThreads(), getMaxThreads()).threadName(getThreadName());
        buildAuthenticationProcessor(def);
        buildChoices(def);
    }

    @Override
    protected void buildChoices(ThreadsDefinition def) {
        ChoiceDefinition choice = def.choice();

        Arrays.asList(MessageGroup.values()).stream().forEach(
                group -> choice.when(exchange -> {
                    String messageType = (String) exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE);
                    return group.getMessageTypeNames().contains(messageType);
                }).to(group.getQueueName()));

        choice.end();
    }

    protected void buildAuthenticationProcessor(ThreadsDefinition def) {
        def.doTry()
                .process(authenticationProcessor)
            .endDoTry()
            .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Message handling failed\nEXCEPTION: ${exception.stacktrace}")
                .process(new AuthenticationFailedProcessor())
                .marshal(getDataFormat(failMessageResponseType.getClazz()))
                .log("Successfully sent '" + failMessageResponseType.name() + "' failed response message: ${body}")
            .end();
    }

    @Override
    protected List<RouteConfiguration> getConfigs() {
        return null;
    }
}
