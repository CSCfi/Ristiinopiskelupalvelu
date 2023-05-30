package fi.uta.ristiinopiskelu.messaging.exception;

public class ObjectConversionException extends RuntimeException {

    public ObjectConversionException(String message) {
        super(message);
    }

    public ObjectConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
