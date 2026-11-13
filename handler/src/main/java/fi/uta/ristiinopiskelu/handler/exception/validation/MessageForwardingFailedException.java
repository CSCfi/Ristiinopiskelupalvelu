package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;

public class MessageForwardingFailedException extends RistiinopiskeluException {
    public MessageForwardingFailedException(String message) {
        super(message, ErrorCode.MESSAGE_FORWARDING_FAILED);
    }

    public MessageForwardingFailedException(String message, Throwable cause) {
        super(message, cause, ErrorCode.MESSAGE_FORWARDING_FAILED);
    }
}
