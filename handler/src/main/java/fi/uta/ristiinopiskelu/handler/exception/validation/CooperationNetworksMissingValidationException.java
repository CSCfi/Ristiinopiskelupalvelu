package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class CooperationNetworksMissingValidationException extends ValidationException {

    public CooperationNetworksMissingValidationException(String message) {
        super(message, ErrorCode.COOPERATION_NETWORKS_MISSING);
    }

    public CooperationNetworksMissingValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.COOPERATION_NETWORKS_MISSING);
    }
}
