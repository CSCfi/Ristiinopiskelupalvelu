package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyElementReferencesMissingValidationException extends ValidationException {
    public StudyElementReferencesMissingValidationException(String message) {
        super(message, ErrorCode.STUDY_ELEMENT_REFERENCES_MISSING);
    }

    public StudyElementReferencesMissingValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.STUDY_ELEMENT_REFERENCES_MISSING);
    }
}
