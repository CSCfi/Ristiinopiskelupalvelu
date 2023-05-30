package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;

public class ForwardedStudyRecordReplyRequest extends ForwardedCreateStudyRecordRequest {

    private StudyRecordStatus status;
    private LocalisedString rejectionReason;

    public ForwardedStudyRecordReplyRequest() {}

    public ForwardedStudyRecordReplyRequest(StudyRecordEntity entity) {
        setStudyRecordRequestId(entity.getId());
        setSendingOrganisation(entity.getSendingOrganisation());
        setReceivingOrganisation(entity.getReceivingOrganisation());
        setStudent(entity.getStudent());
        //Remove personId from reply message
        getStudent().setPersonId(null);
        setCompletedCredits(entity.getCompletedCredits());
        setSendingTime(entity.getSendingTime());
        setRoutingType(entity.getRoutingType());
        setStatus(entity.getStatus());
        setRejectionReason(entity.getRejectionReason());
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
