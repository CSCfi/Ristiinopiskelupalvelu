package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionNotValidInNetworkValidationException extends ValidationException {

    public RegistrationSelectionNotValidInNetworkValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_NOT_VALID_IN_NETWORK);
    }

    public RegistrationSelectionNotValidInNetworkValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_NOT_VALID_IN_NETWORK);
    }
}
