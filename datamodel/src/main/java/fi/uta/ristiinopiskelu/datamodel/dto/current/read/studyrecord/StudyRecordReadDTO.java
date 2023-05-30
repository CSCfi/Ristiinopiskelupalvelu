package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.StudyRecordStudentReadDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "StudyRecord")
public class StudyRecordReadDTO {

    private String id;
    private String sendingOrganisation;
    private String receivingOrganisation;
    private StudyRecordStudentReadDTO student;
    private List<CompletedCredit> completedCredits;
    private OffsetDateTime sendingTime;
    private RoutingType routingType;
    private StudyRecordStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public StudyRecordStudentReadDTO getStudent() {
        return student;
    }

    public void setStudent(StudyRecordStudentReadDTO student) {
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
