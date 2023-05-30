package fi.uta.ristiinopiskelu.handler.exception.validation;

public class RegistrationSelectionSubGroupSelectionMissingException extends ValidationException {
    
    public RegistrationSelectionSubGroupSelectionMissingException(String message) {
        super(message);
    }

    public RegistrationSelectionSubGroupSelectionMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
