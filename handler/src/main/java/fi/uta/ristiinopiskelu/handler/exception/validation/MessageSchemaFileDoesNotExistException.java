package fi.uta.ristiinopiskelu.handler.exception.validation;

public class MessageSchemaFileDoesNotExistException extends ValidationException {

    public MessageSchemaFileDoesNotExistException(String message) {
        super(message);
    }

    public MessageSchemaFileDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
