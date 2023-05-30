package fi.uta.ristiinopiskelu.handler.exception.validation;

public class OrganisationNotValidInNetworkValidationException extends ValidationException {
    public OrganisationNotValidInNetworkValidationException(String message) {
        super(message);
    }

    public OrganisationNotValidInNetworkValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
