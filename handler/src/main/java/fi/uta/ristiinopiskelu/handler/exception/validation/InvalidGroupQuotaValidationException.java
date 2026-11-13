package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class InvalidGroupQuotaValidationException extends ValidationException {
    public InvalidGroupQuotaValidationException(String message) {
        super(message, ErrorCode.INVALID_GROUP_QUOTA);
    }

    public InvalidGroupQuotaValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INVALID_GROUP_QUOTA);
    }
}
