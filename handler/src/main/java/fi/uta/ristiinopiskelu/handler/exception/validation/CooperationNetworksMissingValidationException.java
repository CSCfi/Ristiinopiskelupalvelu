package fi.uta.ristiinopiskelu.handler.exception.validation;

public class CooperationNetworksMissingValidationException extends ValidationException {

    public CooperationNetworksMissingValidationException(String message) {
        super(message);
    }

    public CooperationNetworksMissingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
