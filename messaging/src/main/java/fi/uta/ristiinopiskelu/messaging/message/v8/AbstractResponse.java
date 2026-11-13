package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

public abstract class AbstractResponse implements Message {
    
    private fi.uta.ristiinopiskelu.messaging.message.v8.Status status;
    private String message;
    private Integer errorCode;

    public AbstractResponse() {
        
    }

    public AbstractResponse(fi.uta.ristiinopiskelu.messaging.message.v8.Status status, String message) {
        if(status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
        this.message = message;
    }

    public AbstractResponse(fi.uta.ristiinopiskelu.messaging.message.v8.Status status, String message, ErrorCode errorCode) {
        this(status, message);
        this.errorCode = errorCode != null ? errorCode.getCode() : null;
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

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
}
