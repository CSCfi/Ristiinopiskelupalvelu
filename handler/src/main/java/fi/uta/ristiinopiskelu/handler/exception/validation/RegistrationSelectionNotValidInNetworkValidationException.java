package fi.uta.ristiinopiskelu.handler.exception.validation;

public class RegistrationSelectionNotValidInNetworkValidationException extends ValidationException {

    public RegistrationSelectionNotValidInNetworkValidationException(String message) {
        super(message);
    }

    public RegistrationSelectionNotValidInNetworkValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
