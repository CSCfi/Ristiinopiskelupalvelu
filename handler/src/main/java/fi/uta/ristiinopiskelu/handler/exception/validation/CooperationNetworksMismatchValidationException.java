package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class CooperationNetworksMismatchValidationException extends ValidationException {
    public CooperationNetworksMismatchValidationException(String message) {
        super(message, ErrorCode.COOPERATION_NETWORKS_MISMATCH);
    }

    public CooperationNetworksMismatchValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.COOPERATION_NETWORKS_MISMATCH);
    }
}
