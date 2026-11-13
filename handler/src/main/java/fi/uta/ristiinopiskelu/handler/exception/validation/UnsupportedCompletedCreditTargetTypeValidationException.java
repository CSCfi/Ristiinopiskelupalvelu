package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class UnsupportedCompletedCreditTargetTypeValidationException extends ValidationException {

    public UnsupportedCompletedCreditTargetTypeValidationException(String message) {
        super(message, ErrorCode.COMPLETED_CREDIT_TARGET_TYPE_UNSUPPORTED);
    }

    public UnsupportedCompletedCreditTargetTypeValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.COMPLETED_CREDIT_TARGET_TYPE_UNSUPPORTED);
    }
}
