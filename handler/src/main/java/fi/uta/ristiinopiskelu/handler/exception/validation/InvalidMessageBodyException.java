package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class InvalidMessageBodyException extends ValidationException {
    public InvalidMessageBodyException(String message) {
        super(message, ErrorCode.INVALID_MESSAGE_BODY);
    }

    public InvalidMessageBodyException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INVALID_MESSAGE_BODY);
    }
}
