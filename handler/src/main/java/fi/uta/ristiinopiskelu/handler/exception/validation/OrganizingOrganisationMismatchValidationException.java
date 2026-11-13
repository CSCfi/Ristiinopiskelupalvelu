package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class OrganizingOrganisationMismatchValidationException extends ValidationException {
    public OrganizingOrganisationMismatchValidationException(String message) {
        super(message, ErrorCode.ORGANIZING_ORGANISATION_MISMATCH);
    }

    public OrganizingOrganisationMismatchValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.ORGANIZING_ORGANISATION_MISMATCH);
    }
}
