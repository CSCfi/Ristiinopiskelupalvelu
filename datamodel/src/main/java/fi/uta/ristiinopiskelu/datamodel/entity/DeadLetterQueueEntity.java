package fi.uta.ristiinopiskelu.datamodel.entity;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Document(indexName = "deadletterqueue", createIndex = false)
public class DeadLetterQueueEntity extends GenericEntity implements Serializable {
    private String message;
    private String organisationId;
    private Boolean emailSent;
    private String messageType;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime consumedTimestamp;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public OffsetDateTime getConsumedTimestamp() {
        return consumedTimestamp;
    }

    public void setConsumedTimestamp(OffsetDateTime consumedTimestamp) {
        this.consumedTimestamp = consumedTimestamp;
    }
}
