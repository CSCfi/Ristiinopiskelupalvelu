package fi.uta.ristiinopiskelu.handler.exception.validation;

public class InvalidMessageBodyException extends ValidationException {
    public InvalidMessageBodyException(String message) {
        super(message);
    }

    public InvalidMessageBodyException(String message, Throwable cause) {
        super(message, cause);
    }
}
