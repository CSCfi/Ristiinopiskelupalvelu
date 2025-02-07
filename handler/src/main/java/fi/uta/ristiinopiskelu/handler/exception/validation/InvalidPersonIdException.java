package fi.uta.ristiinopiskelu.handler.exception.validation;

public class InvalidPersonIdException extends ValidationException {

    public InvalidPersonIdException(String message) {
        super(message);
    }

    public InvalidPersonIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
