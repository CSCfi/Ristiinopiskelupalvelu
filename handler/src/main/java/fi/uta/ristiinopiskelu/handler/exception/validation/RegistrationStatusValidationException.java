package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationStatusValidationException extends ValidationException {
    public RegistrationStatusValidationException (String message){
        super(message, ErrorCode.REGISTRATION_STATUS_INVALID);
    }
}
