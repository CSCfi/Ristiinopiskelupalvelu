package fi.uta.ristiinopiskelu.dlqhandler.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.List;

public abstract class AbstractRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRoute.class);

    @Value("${general.activemq.dlq-queue-pattern}")
    private String dlqQueuePattern;

    @Value("${general.camel.redeliveryDelay}")
    private long redeliveryDelay;

    @Value("${general.camel.redeliveryErrorLoggingDelay}")
    private long redeliveryErrorLoggingDelay;

    protected abstract List<RouteConfiguration> getConfigs();

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .onExceptionOccurred(exchange -> {
                Integer redeliveryCounter = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
                String queueName = exchange.getFromEndpoint().getEndpointUri();
                // Do not spam logs with error messages. Write first encounter with full stacktrace and then error after every ~half minute
                // (do not log full stacktrace since camel suppresses all errors and after a while stack trace will be VERY long)
                if(redeliveryCounter != null && redeliveryCounter == 1) {
                    logger.error("Error handling dead letter queue: \"" + queueName + "\" Starting to redeliver.", exchange.getException());
                } else if(redeliveryCounter != null && ((redeliveryCounter * redeliveryDelay) % redeliveryErrorLoggingDelay <= redeliveryDelay)) {
                    logger.error("Error in handling dead letter queue: \"" + queueName + "\" " + exchange.getException().getClass().getName() + " " + exchange.getException().getMessage());
                }
            })
            .maximumRedeliveries(-1)
            .redeliveryDelay(redeliveryDelay);

        if(!CollectionUtils.isEmpty(this.getConfigs())) {
            this.getConfigs().forEach(config -> buildRoute(config));
        }
    }

    protected void buildRoute(RouteConfiguration config) {
        from(String.format(dlqQueuePattern, config.getOrganisationQueue()))
            .transacted()
            .process(config.getDeadLetterQueueProcessor());
    }
}
