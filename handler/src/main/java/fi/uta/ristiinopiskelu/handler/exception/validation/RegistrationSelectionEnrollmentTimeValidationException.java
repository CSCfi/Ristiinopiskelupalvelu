package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionEnrollmentTimeValidationException extends ValidationException {

    public RegistrationSelectionEnrollmentTimeValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_ENROLLMENT_TIME_INVALID);
    }

    public RegistrationSelectionEnrollmentTimeValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_ENROLLMENT_TIME_INVALID);
    }
}
