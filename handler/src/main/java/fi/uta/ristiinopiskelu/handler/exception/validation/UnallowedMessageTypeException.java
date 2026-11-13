package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class UnallowedMessageTypeException extends ValidationException {

    public UnallowedMessageTypeException(String message) {
        super(message, ErrorCode.UNALLOWED_MESSAGE_TYPE);
    }

    public UnallowedMessageTypeException(String message, Throwable cause) {
        super(message, cause, ErrorCode.UNALLOWED_MESSAGE_TYPE);
    }
}
