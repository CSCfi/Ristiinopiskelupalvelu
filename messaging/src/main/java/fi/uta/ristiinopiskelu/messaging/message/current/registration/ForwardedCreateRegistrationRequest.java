package fi.uta.ristiinopiskelu.messaging.message.current.registration;

public class ForwardedCreateRegistrationRequest extends CreateRegistrationRequest {

    private String registrationRequestId;

    public String getRegistrationRequestId() {
        return registrationRequestId;
    }

    public void setRegistrationRequestId(String registrationRequestId) {
        this.registrationRequestId = registrationRequestId;
    }
}
