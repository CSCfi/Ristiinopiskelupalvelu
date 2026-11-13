package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class DefaultResponse extends AbstractResponse {
    public DefaultResponse(Status status, String message) {
        super(status, message);
    }

    public DefaultResponse(Status status, String message, ErrorCode errorCode) {
        super(status, message, errorCode);
    }

    public DefaultResponse() {}
}
