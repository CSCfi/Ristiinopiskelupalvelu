package fi.uta.ristiinopiskelu.handler.exception.validation;

public class NetworkOrganisationValidationException extends ValidationException {

    public NetworkOrganisationValidationException(String message) {
        super(message);
    }

    public NetworkOrganisationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
