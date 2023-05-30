package fi.uta.ristiinopiskelu.messaging.message.current;

import org.springframework.util.Assert;

import java.time.OffsetDateTime;

public abstract class AbstractNotification implements Notification {

    private String sendingOrganisationTkCode;
    private OffsetDateTime timestamp;

    public AbstractNotification() {
        
    }

    public AbstractNotification(String sendingOrganisationTkCode, OffsetDateTime timestamp) {
        Assert.hasText(sendingOrganisationTkCode, "sendingOrganisationTkCode cannot be empty");
        Assert.notNull(timestamp, "Timestamp cannot be null");
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
        this.timestamp = timestamp;
    }

    @Override
    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }

    @Override
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
