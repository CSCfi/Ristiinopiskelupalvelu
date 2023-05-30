package fi.uta.ristiinopiskelu.handler.exception.validation;

public class InvalidGroupQuotaValidationException extends ValidationException {
    public InvalidGroupQuotaValidationException(String message) {
        super(message);
    }

    public InvalidGroupQuotaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
