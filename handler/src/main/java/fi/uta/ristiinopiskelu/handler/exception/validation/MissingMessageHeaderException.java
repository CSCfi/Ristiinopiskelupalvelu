package fi.uta.ristiinopiskelu.handler.exception.validation;

public class MissingMessageHeaderException extends ValidationException {
    public MissingMessageHeaderException(String message) {
        super(message);
    }

    public MissingMessageHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
