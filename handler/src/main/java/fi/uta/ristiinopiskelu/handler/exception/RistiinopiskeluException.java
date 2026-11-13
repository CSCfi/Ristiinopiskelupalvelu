package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RistiinopiskeluException extends RuntimeException {

    private final ErrorCode errorCode;

    public RistiinopiskeluException(String message) {
        this(message, ErrorCode.INTERNAL_ERROR);
    }

    public RistiinopiskeluException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RistiinopiskeluException(String message, Throwable cause) {
        this(message, cause, ErrorCode.INTERNAL_ERROR);
    }

    public RistiinopiskeluException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
