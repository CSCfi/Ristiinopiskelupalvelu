package fi.uta.ristiinopiskelu.handler.exception;

public class DataConversionException extends RistiinopiskeluException {
    public DataConversionException(String message) {
        super(message);
    }

    public DataConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
