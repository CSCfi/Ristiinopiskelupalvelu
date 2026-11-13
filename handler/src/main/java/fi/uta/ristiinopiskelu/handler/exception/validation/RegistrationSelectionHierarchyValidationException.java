package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionHierarchyValidationException extends ValidationException {

    public RegistrationSelectionHierarchyValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_HIERARCHY_INVALID);
    }

    public RegistrationSelectionHierarchyValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_HIERARCHY_INVALID);
    }
}
