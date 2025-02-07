package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;

public class DefaultErrorProcessor extends AbstractErrorProcessor {

    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        return new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.FAILED, "Failed to process message with type %s, error: %s"
                        .formatted(exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE), exception.getMessage())));
    }
}
