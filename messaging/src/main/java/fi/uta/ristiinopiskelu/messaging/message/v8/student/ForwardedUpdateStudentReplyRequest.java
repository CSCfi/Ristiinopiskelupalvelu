package fi.uta.ristiinopiskelu.messaging.message.v8.student;

import fi.uta.ristiinopiskelu.messaging.message.v8.student.StudentReplyRequest;

public class ForwardedUpdateStudentReplyRequest extends StudentReplyRequest {

    private String sendingOrganisationTkCode;

    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }
}
