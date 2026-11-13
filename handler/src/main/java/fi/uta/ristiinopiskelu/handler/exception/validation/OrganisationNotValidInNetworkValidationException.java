package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class OrganisationNotValidInNetworkValidationException extends ValidationException {
    public OrganisationNotValidInNetworkValidationException(String message) {
        super(message, ErrorCode.ORGANISATION_NOT_VALID_IN_NETWORK);
    }

    public OrganisationNotValidInNetworkValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.ORGANISATION_NOT_VALID_IN_NETWORK);
    }
}
