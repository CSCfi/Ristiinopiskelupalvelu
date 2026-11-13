package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionSubGroupSelectionMissingException extends ValidationException {
    
    public RegistrationSelectionSubGroupSelectionMissingException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_SELECTION_MISSING);
    }

    public RegistrationSelectionSubGroupSelectionMissingException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_SELECTION_MISSING);
    }
}
