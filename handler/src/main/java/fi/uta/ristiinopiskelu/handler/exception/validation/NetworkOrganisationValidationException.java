package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class NetworkOrganisationValidationException extends ValidationException {

    public NetworkOrganisationValidationException(String message) {
        super(message, ErrorCode.NETWORK_ORGANISATION_INVALID);
    }

    public NetworkOrganisationValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.NETWORK_ORGANISATION_INVALID);
    }
}
