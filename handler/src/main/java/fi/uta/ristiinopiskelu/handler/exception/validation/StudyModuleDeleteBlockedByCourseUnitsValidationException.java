package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyModuleDeleteBlockedByCourseUnitsValidationException extends ValidationException {

    public StudyModuleDeleteBlockedByCourseUnitsValidationException(String message) {
        super(message, ErrorCode.STUDY_MODULE_DELETE_BLOCKED_BY_COURSE_UNITS);
    }
}
