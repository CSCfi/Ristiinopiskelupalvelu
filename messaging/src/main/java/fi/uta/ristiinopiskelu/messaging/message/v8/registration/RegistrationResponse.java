package fi.uta.ristiinopiskelu.messaging.message.v8.registration;

import fi.uta.ristiinopiskelu.messaging.message.v8.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

/**
 * Used when handler instantly responds to the sender about a CREATE_REGISTRATION_REQUEST message
 */
public class RegistrationResponse extends DefaultResponse {

    private String registrationRequestId;

    public RegistrationResponse(Status status, String message, String registrationRequestId) {
        super(status, message);
        this.registrationRequestId = registrationRequestId;
    }

    public RegistrationResponse() {
    }

    public String getRegistrationRequestId() {
        return registrationRequestId;
    }

    public void setRegistrationRequestId(String registrationRequestId) {
        this.registrationRequestId = registrationRequestId;
    }
}
