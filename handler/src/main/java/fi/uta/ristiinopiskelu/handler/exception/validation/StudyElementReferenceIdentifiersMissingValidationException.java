package fi.uta.ristiinopiskelu.handler.exception.validation;

public class StudyElementReferenceIdentifiersMissingValidationException extends ValidationException {

    public StudyElementReferenceIdentifiersMissingValidationException(String message) {
        super(message);
    }

    public StudyElementReferenceIdentifiersMissingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
