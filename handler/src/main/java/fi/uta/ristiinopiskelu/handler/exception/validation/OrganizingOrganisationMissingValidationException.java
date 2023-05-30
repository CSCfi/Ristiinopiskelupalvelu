package fi.uta.ristiinopiskelu.handler.exception.validation;

public class OrganizingOrganisationMissingValidationException extends ValidationException {
    public OrganizingOrganisationMissingValidationException(String message) {
        super(message);
    }

    public OrganizingOrganisationMissingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
