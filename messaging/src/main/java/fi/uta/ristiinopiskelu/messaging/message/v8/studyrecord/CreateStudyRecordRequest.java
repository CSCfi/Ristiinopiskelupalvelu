package fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractPersonIdentifiableRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.RoutingType;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

public class CreateStudyRecordRequest extends AbstractPersonIdentifiableRequest {
    private String sendingOrganisation;
    private String receivingOrganisation;
    private StudyRecordStudent student;
    private List<CompletedCredit> completedCredits;
    private OffsetDateTime sendingTime;
    private RoutingType routingType;

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

    @JsonIgnore
    @Override
    public String getPersonIdentifier() {
        if(getStudent() != null) {
            return !StringUtils.isEmpty(getStudent().getPersonId()) ? getStudent().getPersonId() : getStudent().getOid();
        }

        return null;
    }
}
