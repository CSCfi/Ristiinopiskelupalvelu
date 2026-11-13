package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class MessageSchemaFileDoesNotExistException extends ValidationException {

    public MessageSchemaFileDoesNotExistException(String message) {
        super(message, ErrorCode.MESSAGE_SCHEMA_FILE_DOES_NOT_EXIST);
    }

    public MessageSchemaFileDoesNotExistException(String message, Throwable cause) {
        super(message, cause, ErrorCode.MESSAGE_SCHEMA_FILE_DOES_NOT_EXIST);
    }
}
