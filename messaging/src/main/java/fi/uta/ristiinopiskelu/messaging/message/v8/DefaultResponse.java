package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

public class DefaultResponse extends AbstractResponse {
    public DefaultResponse(Status status, String message) {
        super(status, message);
    }

    public DefaultResponse() {}
}
