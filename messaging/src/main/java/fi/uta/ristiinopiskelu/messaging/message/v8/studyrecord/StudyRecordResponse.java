package fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord;

import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

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
