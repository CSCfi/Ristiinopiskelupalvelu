package fi.uta.ristiinopiskelu.messaging.message.current.student;

public class ForwardedUpdateStudentStudyRightReplyRequest extends StudentReplyRequest {

    private String sendingOrganisationTkCode;

    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }
}
