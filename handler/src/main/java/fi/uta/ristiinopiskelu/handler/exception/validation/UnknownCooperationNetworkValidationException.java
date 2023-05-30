package fi.uta.ristiinopiskelu.handler.exception.validation;

public class UnknownCooperationNetworkValidationException extends ValidationException {
    public UnknownCooperationNetworkValidationException(String message) {
        super(message);
    }

    public UnknownCooperationNetworkValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
