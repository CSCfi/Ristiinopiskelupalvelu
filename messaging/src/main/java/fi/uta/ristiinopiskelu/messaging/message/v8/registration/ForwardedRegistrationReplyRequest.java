package fi.uta.ristiinopiskelu.messaging.message.v8.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationStatus;

public class ForwardedRegistrationReplyRequest extends ForwardedCreateRegistrationRequest {
    
    private RegistrationStatus status;
    private LocalisedString rejectionReason;

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
