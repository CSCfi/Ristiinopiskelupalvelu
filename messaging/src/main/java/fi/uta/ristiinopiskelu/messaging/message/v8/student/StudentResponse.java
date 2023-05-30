package fi.uta.ristiinopiskelu.messaging.message.v8.student;

import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

/**
 * Used when handler instantly responds to the sender about a CANCEL_STUDENT_REQUEST or UPDATE_STUDENT_REQUEST message
 */
public class StudentResponse extends DefaultResponse {

    private String studentRequestId;

    public StudentResponse(Status status, String message, String studentRequestId) {
        super(status, message);
        this.studentRequestId = studentRequestId;
    }

    public StudentResponse() {
    }

    public String getStudentRequestId() {
        return studentRequestId;
    }

    public void setStudentRequestId(String studentRequestId) {
        this.studentRequestId = studentRequestId;
    }
}
