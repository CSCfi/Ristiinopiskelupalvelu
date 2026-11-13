package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class ReferencedStudyElementMissingValidationException extends ValidationException {
    public ReferencedStudyElementMissingValidationException(String message) {
        super(message, ErrorCode.REFERENCED_STUDY_ELEMENT_MISSING);
    }

    public ReferencedStudyElementMissingValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REFERENCED_STUDY_ELEMENT_MISSING);
    }
}
