package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.*;
import org.apache.camel.Exchange;

public class AuthenticationFailedProcessor extends AbstractErrorProcessor {
    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        String jmsxUserId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        return new RistiinopiskeluMessage(exchange,
                MessageType.AUTHENTICATION_FAILED_RESPONSE,
                new DefaultResponse(Status.FAILED, "Failed to process message with type " + exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE)
                        + ". Authentication failed. Unable to parse given JMSXUserId: " + jmsxUserId));
    }
}