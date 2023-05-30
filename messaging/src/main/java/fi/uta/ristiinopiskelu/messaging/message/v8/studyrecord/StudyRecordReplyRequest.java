package fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStatus;

public class StudyRecordReplyRequest extends AbstractRequest {

    private String studyRecordRequestId;
    private StudyRecordStatus status;
    private LocalisedString rejectionReason;

    public String getStudyRecordRequestId() {
        return studyRecordRequestId;
    }

    public void setStudyRecordRequestId(String studyRecordRequestId) {
        this.studyRecordRequestId = studyRecordRequestId;
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
