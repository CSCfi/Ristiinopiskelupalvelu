package fi.uta.ristiinopiskelu.messaging.message.current.student;

public class ForwardedUpdateStudentStudyRightReplyRequest extends StudentReplyRequest {

    private String sendingOrganisationTkCode;

    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }

    public static ForwardedUpdateStudentStudyRightReplyRequest from(UpdateStudentStudyRightReplyRequest source) {
        if (source == null) {
            return null;
        }

        ForwardedUpdateStudentStudyRightReplyRequest target = new ForwardedUpdateStudentStudyRightReplyRequest();
        target.setStudentRequestId(source.getStudentRequestId());
        target.setStatus(source.getStatus());
        target.setRejectionReason(source.getRejectionReason());
        return target;
    }
}
