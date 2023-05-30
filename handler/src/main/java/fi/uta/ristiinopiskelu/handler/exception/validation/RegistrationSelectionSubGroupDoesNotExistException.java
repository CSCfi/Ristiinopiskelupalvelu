package fi.uta.ristiinopiskelu.handler.exception.validation;

public class RegistrationSelectionSubGroupDoesNotExistException extends ValidationException {
    
    public RegistrationSelectionSubGroupDoesNotExistException(String message) {
        super(message);
    }

    public RegistrationSelectionSubGroupDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
