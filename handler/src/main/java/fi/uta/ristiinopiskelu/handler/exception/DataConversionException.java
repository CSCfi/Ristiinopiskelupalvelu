package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class DataConversionException extends RistiinopiskeluException {
    public DataConversionException(String message) {
        super(message, ErrorCode.DATA_CONVERSION_FAILED);
    }

    public DataConversionException(String message, Throwable cause) {
        super(message, cause, ErrorCode.DATA_CONVERSION_FAILED);
    }
}
