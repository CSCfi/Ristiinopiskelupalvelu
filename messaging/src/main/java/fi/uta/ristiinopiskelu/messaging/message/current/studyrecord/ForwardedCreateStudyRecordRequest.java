package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

public class ForwardedCreateStudyRecordRequest extends CreateStudyRecordRequest {

    private String studyRecordRequestId;

    public String getStudyRecordRequestId() {
        return studyRecordRequestId;
    }

    public void setStudyRecordRequestId(String studyRecordRequestId) {
        this.studyRecordRequestId = studyRecordRequestId;
    }

    public static ForwardedCreateStudyRecordRequest from(CreateStudyRecordRequest source) {
        if (source == null) {
            return null;
        }

        ForwardedCreateStudyRecordRequest target = new ForwardedCreateStudyRecordRequest();
        target.setSendingOrganisation(source.getSendingOrganisation());
        target.setReceivingOrganisation(source.getReceivingOrganisation());
        target.setNetworkIdentifier(source.getNetworkIdentifier());
        target.setStudent(source.getStudent());
        target.setCompletedCredits(source.getCompletedCredits());
        target.setSendingTime(source.getSendingTime());
        target.setRoutingType(source.getRoutingType());
        return target;
    }
}
