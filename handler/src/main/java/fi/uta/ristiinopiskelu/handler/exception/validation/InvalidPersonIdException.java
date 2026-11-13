package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class InvalidPersonIdException extends ValidationException {

    public InvalidPersonIdException(String message) {
        super(message, ErrorCode.INVALID_PERSON_ID);
    }

    public InvalidPersonIdException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INVALID_PERSON_ID);
    }
}
