package fi.uta.ristiinopiskelu.handler.exception.validation;

public class CooperationNetworksMismatchValidationException extends ValidationException {
    public CooperationNetworksMismatchValidationException(String message) {
        super(message);
    }

    public CooperationNetworksMismatchValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
