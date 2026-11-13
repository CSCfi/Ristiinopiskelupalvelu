package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class MultipleOrganizingOrganisationValidationException extends ValidationException {
    public MultipleOrganizingOrganisationValidationException(String message) {
        super(message, ErrorCode.MULTIPLE_ORGANIZING_ORGANISATIONS);
    }

    public MultipleOrganizingOrganisationValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.MULTIPLE_ORGANIZING_ORGANISATIONS);
    }
}
