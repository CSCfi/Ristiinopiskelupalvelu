package fi.uta.ristiinopiskelu.handler.exception.validation;

public class NoForwardingOrganisationsValidationException extends ValidationException {
    public NoForwardingOrganisationsValidationException(String message) {
        super(message);
    }

    public NoForwardingOrganisationsValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
