package fi.uta.ristiinopiskelu.messaging.message.v8.registration;

public class ForwardedCreateRegistrationRequest extends CreateRegistrationRequest {

    private String registrationRequestId;

    public String getRegistrationRequestId() {
        return registrationRequestId;
    }

    public void setRegistrationRequestId(String registrationRequestId) {
        this.registrationRequestId = registrationRequestId;
    }
}
