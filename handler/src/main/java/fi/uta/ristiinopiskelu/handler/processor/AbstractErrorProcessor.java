package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractErrorProcessor implements Processor {

    public abstract RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception);

    private static final Logger logger = LoggerFactory.getLogger(AbstractErrorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        final Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        exchange.setMessage(createErrorResponse(exchange, exception));
    }
}
