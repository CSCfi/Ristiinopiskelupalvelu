package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;

public class UnallowedMessageTypeException extends ValidationException {

    public UnallowedMessageTypeException(String message) {
        super(message);
    }

    public UnallowedMessageTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
