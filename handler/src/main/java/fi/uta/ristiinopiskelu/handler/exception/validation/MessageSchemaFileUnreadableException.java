package fi.uta.ristiinopiskelu.handler.exception.validation;

public class MessageSchemaFileUnreadableException extends ValidationException {

    public MessageSchemaFileUnreadableException(String message) {
        super(message);
    }

    public MessageSchemaFileUnreadableException(String message, Throwable cause) {
        super(message, cause);
    }
}
