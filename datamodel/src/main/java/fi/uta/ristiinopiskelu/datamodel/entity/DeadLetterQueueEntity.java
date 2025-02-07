package fi.uta.ristiinopiskelu.datamodel.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Document(indexName = "deadletterqueue", createIndex = false)
public class DeadLetterQueueEntity extends GenericEntity implements Serializable {
    private String message;
    private String organisationId;
    private Boolean emailSent;
    private String messageType;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime consumedTimestamp;

    @Builder
    public DeadLetterQueueEntity(String id, Long version, String message, String organisationId, Boolean emailSent,
                                 String messageType, OffsetDateTime consumedTimestamp) {
        super(id, version, null, null, null);
        this.message = message;
        this.organisationId = organisationId;
        this.emailSent = emailSent;
        this.messageType = messageType;
        this.consumedTimestamp = consumedTimestamp;
    }
}
