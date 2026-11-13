package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.RoutingTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.StudyRecordStatusEntity;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "opintosuoritukset", createIndex = false)
public class StudyRecordEntity extends GenericEntity {

    private String sendingOrganisation;
    private String receivingOrganisation;
    private String networkIdentifier;
    private StudyRecordStudentEntity student;
    private List<CompletedCreditEntity> completedCredits;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendingTime;
    
    private RoutingTypeEntity routingType;
    private StudyRecordStatusEntity status;
    private LocalisedStringEntity rejectionReason;

    public StudyRecordEntity(StudyRecordEntity old) {
        this.sendingOrganisation = old.getSendingOrganisation();
        this.receivingOrganisation = old.getReceivingOrganisation();
        this.networkIdentifier = old.getNetworkIdentifier();
        this.student = old.getStudent();
        this.completedCredits = old.getCompletedCredits();
        this.sendingTime = old.getSendingTime();
        this.routingType = old.getRoutingType();
        this.status = old.getStatus();
    }
}
