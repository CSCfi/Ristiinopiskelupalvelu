package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionDoesNotExistValidationException extends ValidationException {

    public RegistrationSelectionDoesNotExistValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_DOES_NOT_EXIST);
    }

    public RegistrationSelectionDoesNotExistValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_DOES_NOT_EXIST);
    }
}
