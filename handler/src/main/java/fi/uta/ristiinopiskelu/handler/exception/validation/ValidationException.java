package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class ValidationException extends RistiinopiskeluException {

    public ValidationException(String message) {
        super(message, ErrorCode.VALIDATION_FAILED);
    }

    public ValidationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.VALIDATION_FAILED);
    }

    public ValidationException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause, errorCode);
    }
}
