package fi.uta.ristiinopiskelu.handler.exception.validation;

public class NotMemberOfCooperationNetworkValidationException extends ValidationException {
    public NotMemberOfCooperationNetworkValidationException(String message) {
        super(message);
    }

    public NotMemberOfCooperationNetworkValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
