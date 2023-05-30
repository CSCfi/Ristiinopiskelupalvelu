package fi.uta.ristiinopiskelu.handler.exception.validation;

public class StudyElementReferencesMissingValidationException extends ValidationException {
    public StudyElementReferencesMissingValidationException(String message) {
        super(message);
    }

    public StudyElementReferencesMissingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
