package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

public abstract class AbstractResponse implements Message {
    
    private fi.uta.ristiinopiskelu.messaging.message.v8.Status status;
    private String message;

    public AbstractResponse() {
        
    }

    public AbstractResponse(fi.uta.ristiinopiskelu.messaging.message.v8.Status status, String message) {
        if(status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
        this.message = message;
    }

    public fi.uta.ristiinopiskelu.messaging.message.v8.Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
