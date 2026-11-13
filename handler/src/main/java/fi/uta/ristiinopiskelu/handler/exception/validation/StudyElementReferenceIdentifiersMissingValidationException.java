package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyElementReferenceIdentifiersMissingValidationException extends ValidationException {

    public StudyElementReferenceIdentifiersMissingValidationException(String message) {
        super(message, ErrorCode.STUDY_ELEMENT_REFERENCE_IDENTIFIERS_MISSING);
    }

    public StudyElementReferenceIdentifiersMissingValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.STUDY_ELEMENT_REFERENCE_IDENTIFIERS_MISSING);
    }
}
