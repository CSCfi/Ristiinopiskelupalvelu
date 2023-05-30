package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import java.time.OffsetDateTime;
import java.util.List;

public class StudyRecord {

    private String sendingOrganisation;
    private String receivingOrganisation;
    private StudyRecordStudent student;
    private List<CompletedCredit> completedCredits;
    private OffsetDateTime sendingTime;
    private RoutingType routingType;
    private StudyRecordStatus status;

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
}
