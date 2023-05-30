package fi.uta.ristiinopiskelu.messaging.message.current.student;

public class ForwardedUpdateStudentReplyRequest extends StudentReplyRequest {

    private String sendingOrganisationTkCode;

    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }
}
