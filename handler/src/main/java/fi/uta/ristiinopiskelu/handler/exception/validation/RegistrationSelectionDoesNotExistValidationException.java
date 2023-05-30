package fi.uta.ristiinopiskelu.handler.exception.validation;

public class RegistrationSelectionDoesNotExistValidationException extends ValidationException {

    public RegistrationSelectionDoesNotExistValidationException(String message) {
        super(message);
    }

    public RegistrationSelectionDoesNotExistValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
