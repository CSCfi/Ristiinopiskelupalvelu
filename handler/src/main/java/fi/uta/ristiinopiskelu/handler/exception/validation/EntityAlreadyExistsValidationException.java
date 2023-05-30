package fi.uta.ristiinopiskelu.handler.exception.validation;

public class EntityAlreadyExistsValidationException extends ValidationException {

    public EntityAlreadyExistsValidationException(String message) {
        super(message);
    }

    public EntityAlreadyExistsValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
