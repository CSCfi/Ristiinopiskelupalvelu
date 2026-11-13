package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionReferenceValidationException extends ValidationException {

    public RegistrationSelectionReferenceValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_REFERENCE_INVALID);
    }

    public RegistrationSelectionReferenceValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_REFERENCE_INVALID);
    }
}
