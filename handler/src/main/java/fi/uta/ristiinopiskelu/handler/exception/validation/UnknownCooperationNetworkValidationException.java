package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class UnknownCooperationNetworkValidationException extends ValidationException {
    public UnknownCooperationNetworkValidationException(String message) {
        super(message, ErrorCode.UNKNOWN_COOPERATION_NETWORK);
    }

    public UnknownCooperationNetworkValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.UNKNOWN_COOPERATION_NETWORK);
    }
}
