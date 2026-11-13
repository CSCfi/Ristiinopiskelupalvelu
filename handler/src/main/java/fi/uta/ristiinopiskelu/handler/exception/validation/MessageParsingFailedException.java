package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class MessageParsingFailedException extends ValidationException {

    public MessageParsingFailedException(String message) {
        super(message, ErrorCode.MESSAGE_PARSING_FAILED);
    }

    public MessageParsingFailedException(String message, Throwable cause) {
        super(message, cause, ErrorCode.MESSAGE_PARSING_FAILED);
    }
}
