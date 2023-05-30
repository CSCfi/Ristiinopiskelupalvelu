package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;

public class MessageForwardingFailedException extends RistiinopiskeluException {
    public MessageForwardingFailedException(String message) {
        super(message);
    }

    public MessageForwardingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
