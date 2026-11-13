package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class InvalidMessageHeaderException extends ValidationException {

    public InvalidMessageHeaderException(String message) {
        super(message, ErrorCode.INVALID_MESSAGE_HEADER);
    }

    public InvalidMessageHeaderException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INVALID_MESSAGE_HEADER);
    }
}
