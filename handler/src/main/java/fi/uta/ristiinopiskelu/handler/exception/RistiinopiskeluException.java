package fi.uta.ristiinopiskelu.handler.exception;

public class RistiinopiskeluException extends RuntimeException {

    public RistiinopiskeluException(String message) {
        super(message);
    }

    public RistiinopiskeluException(String message, Throwable cause) {
        super(message, cause);
    }
}
