package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class OrganizingOrganisationMissingValidationException extends ValidationException {
    public OrganizingOrganisationMissingValidationException(String message) {
        super(message, ErrorCode.ORGANIZING_ORGANISATION_MISSING);
    }

    public OrganizingOrganisationMissingValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.ORGANIZING_ORGANISATION_MISSING);
    }
}
