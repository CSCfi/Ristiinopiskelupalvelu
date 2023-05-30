package fi.uta.ristiinopiskelu.messaging.message.current;

public class DefaultResponse extends AbstractResponse {
    public DefaultResponse(Status status, String message) {
        super(status, message);
    }

    public DefaultResponse() {}
}
