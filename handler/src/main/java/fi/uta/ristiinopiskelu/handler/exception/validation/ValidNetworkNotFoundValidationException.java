package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class ValidNetworkNotFoundValidationException extends ValidationException {
    public ValidNetworkNotFoundValidationException(String message) {
        super(message, ErrorCode.VALID_NETWORK_NOT_FOUND);
    }

    public ValidNetworkNotFoundValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.VALID_NETWORK_NOT_FOUND);
    }
}
