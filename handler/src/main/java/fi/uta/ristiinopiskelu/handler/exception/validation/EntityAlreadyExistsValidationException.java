package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class EntityAlreadyExistsValidationException extends ValidationException {

    public EntityAlreadyExistsValidationException(String message) {
        super(message, ErrorCode.ENTITY_ALREADY_EXISTS);
    }

    public EntityAlreadyExistsValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.ENTITY_ALREADY_EXISTS);
    }
}
