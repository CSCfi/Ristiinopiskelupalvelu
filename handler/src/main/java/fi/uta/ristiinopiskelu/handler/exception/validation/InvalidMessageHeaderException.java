package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;

public class InvalidMessageHeaderException extends ValidationException {

    public InvalidMessageHeaderException(String message) {
        super(message);
    }

    public InvalidMessageHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
