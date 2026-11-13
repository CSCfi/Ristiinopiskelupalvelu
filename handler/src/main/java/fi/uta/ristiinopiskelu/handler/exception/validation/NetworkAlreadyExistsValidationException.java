package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class NetworkAlreadyExistsValidationException extends ValidationException {

    public NetworkAlreadyExistsValidationException(String message) {
        super(message, ErrorCode.NETWORK_ALREADY_EXISTS);
    }
}
