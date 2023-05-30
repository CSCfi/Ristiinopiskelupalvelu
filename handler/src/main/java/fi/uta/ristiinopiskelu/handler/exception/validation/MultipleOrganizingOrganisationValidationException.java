package fi.uta.ristiinopiskelu.handler.exception.validation;

public class MultipleOrganizingOrganisationValidationException extends ValidationException {
    public MultipleOrganizingOrganisationValidationException(String message) {
        super(message);
    }

    public MultipleOrganizingOrganisationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
