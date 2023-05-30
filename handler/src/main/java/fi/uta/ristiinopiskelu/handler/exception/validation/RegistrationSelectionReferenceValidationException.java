package fi.uta.ristiinopiskelu.handler.exception.validation;

public class RegistrationSelectionReferenceValidationException extends ValidationException {

    public RegistrationSelectionReferenceValidationException(String message) {
        super(message);
    }

    public RegistrationSelectionReferenceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
