package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class NetworkOrganisationsNotFoundValidationException extends ValidationException {

    public NetworkOrganisationsNotFoundValidationException(String message) {
        super(message, ErrorCode.NETWORK_ORGANISATIONS_NOT_FOUND);
    }
}
