package fi.uta.ristiinopiskelu.messaging.message.current.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStatus;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public abstract class StudentReplyRequest extends AbstractRequest {

    private String studentRequestId;
    private StudentStatus status;
    private LocalisedString rejectionReason;

    public StudentReplyRequest() {
    }

    public String getStudentRequestId() {
        return studentRequestId;
    }

    public void setStudentRequestId(String studentRequestId) {
        this.studentRequestId = studentRequestId;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
