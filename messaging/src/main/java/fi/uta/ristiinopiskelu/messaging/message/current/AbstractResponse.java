package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;

public abstract class AbstractResponse implements Message {
    
    private Status status;
    private String message;

    public AbstractResponse() {
        
    }

    public AbstractResponse(Status status, String message) {
        if(status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
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
