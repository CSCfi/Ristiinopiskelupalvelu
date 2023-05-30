package fi.uta.ristiinopiskelu.handler.exception.validation;

public class ReferencedStudyElementMissingValidationException extends ValidationException {
    public ReferencedStudyElementMissingValidationException(String message) {
        super(message);
    }

    public ReferencedStudyElementMissingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
