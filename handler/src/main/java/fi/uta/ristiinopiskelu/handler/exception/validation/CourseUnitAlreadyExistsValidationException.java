package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class CourseUnitAlreadyExistsValidationException extends ValidationException {

    public CourseUnitAlreadyExistsValidationException(String message) {
        super(message, ErrorCode.COURSE_UNIT_ALREADY_EXISTS);
    }
}
