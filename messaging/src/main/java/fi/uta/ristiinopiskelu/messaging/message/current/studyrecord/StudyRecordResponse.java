package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;

/**
 * Used when handler instantly responds to the sender about a CREATE_STUDYRECORD_REQUEST -message
 */
public class StudyRecordResponse extends DefaultResponse {

    private String studyRecordRequestId;

    public StudyRecordResponse(Status status, String message, String studyRecordRequestId) {
        super(status, message);
        this.studyRecordRequestId = studyRecordRequestId;
    }

    public StudyRecordResponse() {
    }

    public String getStudyRecordRequestId() {
        return studyRecordRequestId;
    }

    public void setStudyRecordRequestId(String studyRecordRequestId) {
        this.studyRecordRequestId = studyRecordRequestId;
    }
}
