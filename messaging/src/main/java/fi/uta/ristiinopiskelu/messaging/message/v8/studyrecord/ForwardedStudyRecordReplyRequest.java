package fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStatus;

public class ForwardedStudyRecordReplyRequest extends ForwardedCreateStudyRecordRequest {

    private StudyRecordStatus status;
    private LocalisedString rejectionReason;
    
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
