package fi.uta.ristiinopiskelu.handler.exception.validation;

public class DuplicateEntityValidationException extends ValidationException {

    public DuplicateEntityValidationException(String message) {
        super(message);
    }

    public DuplicateEntityValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
