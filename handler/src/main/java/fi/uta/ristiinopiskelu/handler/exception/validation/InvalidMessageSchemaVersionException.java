package fi.uta.ristiinopiskelu.handler.exception.validation;

public class InvalidMessageSchemaVersionException extends ValidationException {

    public InvalidMessageSchemaVersionException(String message) {
        super(message);
    }

    public InvalidMessageSchemaVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
