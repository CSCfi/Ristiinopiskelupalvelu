package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "opintosuoritukset", createIndex = false)
public class StudyRecordEntity extends GenericEntity {

    private String sendingOrganisation;
    private String receivingOrganisation;
    private String networkIdentifier;
    private StudyRecordStudent student;
    private List<CompletedCredit> completedCredits;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendingTime;
    
    private RoutingType routingType;
    private StudyRecordStatus status;
    private LocalisedString rejectionReason;

    public StudyRecordEntity() {
        
    }

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

    public String getSendingOrganisation() {
        return sendingOrganisation;
    }

    public void setSendingOrganisation(String sendingOrganisation) {
        this.sendingOrganisation = sendingOrganisation;
    }

    public String getReceivingOrganisation() {
        return receivingOrganisation;
    }

    public void setReceivingOrganisation(String receivingOrganisation) {
        this.receivingOrganisation = receivingOrganisation;
    }

    public String getNetworkIdentifier() {
        return networkIdentifier;
    }

    public void setNetworkIdentifier(String networkIdentifier) {
        this.networkIdentifier = networkIdentifier;
    }

    public StudyRecordStudent getStudent() {
        return student;
    }

    public void setStudent(StudyRecordStudent student) {
        this.student = student;
    }

    public List<CompletedCredit> getCompletedCredits() {
        return completedCredits;
    }

    public void setCompletedCredits(List<CompletedCredit> completedCredits) {
        this.completedCredits = completedCredits;
    }

    public OffsetDateTime getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(OffsetDateTime sendingTime) {
        this.sendingTime = sendingTime;
    }

    public RoutingType getRoutingType() {
        return routingType;
    }

    public void setRoutingType(RoutingType routingType) {
        this.routingType = routingType;
    }

    public StudyRecordStatus getStatus() {
        return status;
    }

    public void setStatus(StudyRecordStatus status) {
        this.status = status;
    }

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
