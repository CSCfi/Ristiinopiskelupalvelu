package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;

public class MessageParsingFailedException extends ValidationException {

    public MessageParsingFailedException(String message) {
        super(message);
    }

    public MessageParsingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
