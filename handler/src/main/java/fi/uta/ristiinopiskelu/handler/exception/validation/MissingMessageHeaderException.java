package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class MissingMessageHeaderException extends ValidationException {
    public MissingMessageHeaderException(String headerName) {
        super("Message is missing header: " + headerName, ErrorCode.MISSING_MESSAGE_HEADER);
    }

    public MissingMessageHeaderException(String headerName, Throwable cause) {
        super("Message is missing header: " + headerName, cause, ErrorCode.MISSING_MESSAGE_HEADER);
    }
}
