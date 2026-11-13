package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class DuplicateEntityValidationException extends ValidationException {

    public DuplicateEntityValidationException(String message) {
        super(message, ErrorCode.DUPLICATE_ENTITY);
    }

    public DuplicateEntityValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.DUPLICATE_ENTITY);
    }
}
