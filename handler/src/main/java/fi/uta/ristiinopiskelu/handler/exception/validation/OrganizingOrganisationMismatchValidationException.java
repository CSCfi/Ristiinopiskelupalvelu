package fi.uta.ristiinopiskelu.handler.exception.validation;

public class OrganizingOrganisationMismatchValidationException extends ValidationException {
    public OrganizingOrganisationMismatchValidationException(String message) {
        super(message);
    }

    public OrganizingOrganisationMismatchValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
