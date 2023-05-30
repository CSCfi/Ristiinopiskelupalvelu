package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.*;
import org.apache.camel.Exchange;

public class DefaultErrorProcessor extends AbstractErrorProcessor {
    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        if(exception instanceof RistiinopiskeluException) {
            return new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                    new DefaultResponse(Status.FAILED, "Failed to process message with type " + exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE)
                            + " Error: " + exception.getMessage()
                            + (exception.getCause() != null ? " Caused By: " + exception.getCause() : "")));
        }

        return new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.FAILED, "Failed to process message with type " + exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE)
                        + " Error: " + exception.getMessage()));
    }
}
