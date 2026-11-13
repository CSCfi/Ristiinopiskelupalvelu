package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.current.*;
import org.apache.camel.Exchange;

public class AuthenticationFailedProcessor extends AbstractErrorProcessor {
    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        return new RistiinopiskeluMessage(exchange,
                MessageType.AUTHENTICATION_FAILED_RESPONSE,
                new DefaultResponse(Status.FAILED, ErrorResponseMessageResolver.resolve(exchange, exception),
                    ErrorResponseMessageResolver.resolveErrorCode(exception)));
    }
}
