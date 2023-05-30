package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;

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
