package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class RegistrationSelectionRankException extends ValidationException {

    public RegistrationSelectionRankException (String message){
        super(message, ErrorCode.REGISTRATION_SELECTION_RANK_INVALID);
    }
}
