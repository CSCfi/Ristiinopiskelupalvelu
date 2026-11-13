package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionSubGroupSelectionAmountMismatchValidationException extends ValidationException {

    public RegistrationSelectionSubGroupSelectionAmountMismatchValidationException(String message) {
        super(message, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_SELECTION_AMOUNT_MISMATCH);
    }

    public RegistrationSelectionSubGroupSelectionAmountMismatchValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.REGISTRATION_SELECTION_SUB_GROUP_SELECTION_AMOUNT_MISMATCH);
    }
}
