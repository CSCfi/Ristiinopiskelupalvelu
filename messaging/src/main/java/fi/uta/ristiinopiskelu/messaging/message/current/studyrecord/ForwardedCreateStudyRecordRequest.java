package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

public class ForwardedCreateStudyRecordRequest extends CreateStudyRecordRequest {

    private String studyRecordRequestId;

    public String getStudyRecordRequestId() {
        return studyRecordRequestId;
    }

    public void setStudyRecordRequestId(String studyRecordRequestId) {
        this.studyRecordRequestId = studyRecordRequestId;
    }
}
