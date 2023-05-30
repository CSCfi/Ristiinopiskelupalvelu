package fi.uta.ristiinopiskelu.handler.exception.validation;

public class StudyElementReferenceNotInSameNetworkValidationException extends ValidationException {

    public StudyElementReferenceNotInSameNetworkValidationException(String message) {
        super(message);
    }

    public StudyElementReferenceNotInSameNetworkValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
