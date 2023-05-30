package fi.uta.ristiinopiskelu.handler.exception.validation;

public class ValidNetworkNotFoundValidationException extends ValidationException {
    public ValidNetworkNotFoundValidationException(String message) {
        super(message);
    }

    public ValidNetworkNotFoundValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
