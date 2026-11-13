package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class MessageSchemaFileUnreadableException extends ValidationException {

    public MessageSchemaFileUnreadableException(String message) {
        super(message, ErrorCode.MESSAGE_SCHEMA_FILE_UNREADABLE);
    }

    public MessageSchemaFileUnreadableException(String message, Throwable cause) {
        super(message, cause, ErrorCode.MESSAGE_SCHEMA_FILE_UNREADABLE);
    }
}
