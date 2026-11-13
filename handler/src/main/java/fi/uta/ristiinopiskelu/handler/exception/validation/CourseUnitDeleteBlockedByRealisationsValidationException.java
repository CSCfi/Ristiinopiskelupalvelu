package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class CourseUnitDeleteBlockedByRealisationsValidationException extends ValidationException {

    public CourseUnitDeleteBlockedByRealisationsValidationException(String message) {
        super(message, ErrorCode.COURSE_UNIT_DELETE_BLOCKED_BY_REALISATIONS);
    }
}
