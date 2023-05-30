package fi.uta.ristiinopiskelu.handler.processor;


import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public abstract class AbstractResponseProcessor implements Processor {

    public abstract String getMessage();

    @Override
    public void process(Exchange exchange) throws Exception {
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);
        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE, correlationId,
                new DefaultResponse(Status.OK, getMessage())));
    }
}
