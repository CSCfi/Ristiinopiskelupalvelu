package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionSubGroupDoesNotExistException extends ValidationException {
    
    public RegistrationSelectionSubGroupDoesNotExistException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_DOES_NOT_EXIST);
    }

    public RegistrationSelectionSubGroupDoesNotExistException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_DOES_NOT_EXIST);
    }
}
