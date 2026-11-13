package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;

public class DefaultErrorProcessor extends AbstractErrorProcessor {

    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        ErrorCode errorCode = ErrorResponseMessageResolver.resolveErrorCode(exception);
        return new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.FAILED, ErrorResponseMessageResolver.resolve(exchange, exception), errorCode));
    }

}
