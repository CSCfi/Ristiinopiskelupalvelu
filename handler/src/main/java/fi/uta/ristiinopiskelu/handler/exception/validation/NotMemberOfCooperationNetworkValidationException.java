package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class NotMemberOfCooperationNetworkValidationException extends ValidationException {
    public NotMemberOfCooperationNetworkValidationException(String message) {
        super(message, ErrorCode.NOT_MEMBER_OF_COOPERATION_NETWORK);
    }

    public NotMemberOfCooperationNetworkValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.NOT_MEMBER_OF_COOPERATION_NETWORK);
    }
}
