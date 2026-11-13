package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class InvalidMessageSchemaVersionException extends ValidationException {

    public InvalidMessageSchemaVersionException(String message) {
        super(message, ErrorCode.INVALID_MESSAGE_SCHEMA_VERSION);
    }

    public InvalidMessageSchemaVersionException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INVALID_MESSAGE_SCHEMA_VERSION);
    }
}
