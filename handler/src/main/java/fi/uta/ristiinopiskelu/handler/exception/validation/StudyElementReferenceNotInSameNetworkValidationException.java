package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyElementReferenceNotInSameNetworkValidationException extends ValidationException {

    public StudyElementReferenceNotInSameNetworkValidationException(String message) {
        super(message, ErrorCode.STUDY_ELEMENT_REFERENCE_NOT_IN_SAME_NETWORK);
    }

    public StudyElementReferenceNotInSameNetworkValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.STUDY_ELEMENT_REFERENCE_NOT_IN_SAME_NETWORK);
    }
}
