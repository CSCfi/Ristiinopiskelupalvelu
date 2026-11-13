package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RemovedAssessmentItemHasRealisationReferenceValidationException  extends ValidationException {
    public RemovedAssessmentItemHasRealisationReferenceValidationException(String message) {
        super(message, ErrorCode.REMOVED_ASSESSMENT_ITEM_HAS_REALISATION_REFERENCE);
    }

    public RemovedAssessmentItemHasRealisationReferenceValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REMOVED_ASSESSMENT_ITEM_HAS_REALISATION_REFERENCE);
    }
}
