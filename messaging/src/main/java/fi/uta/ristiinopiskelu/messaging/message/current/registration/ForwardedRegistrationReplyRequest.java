package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;

public class ForwardedRegistrationReplyRequest extends ForwardedCreateRegistrationRequest {
    
    private RegistrationStatus status;
    private LocalisedString rejectionReason;

    public ForwardedRegistrationReplyRequest() {}

    // note that selections are set from entity.selectionsReplies field here
    public ForwardedRegistrationReplyRequest(RegistrationEntity entity) {
        this.setRegistrationRequestId(entity.getId());
        this.setSendingOrganisationTkCode(entity.getSendingOrganisationTkCode());
        this.setReceivingOrganisationTkCode(entity.getReceivingOrganisationTkCode());
        this.setNetworkIdentifier(entity.getNetworkIdentifier());
        this.setNetworkDescription(entity.getNetworkDescription());
        this.setEnrolmentDateTime(entity.getEnrolmentDateTime());
        this.setStudent(entity.getStudent());
        //Remove personId from reply message
        this.getStudent().setPersonId(null);
        this.setSelections(entity.getSelectionsReplies());
        this.setStatus(entity.getStatus());
        this.setRejectionReason(entity.getRejectionReason());
    }

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
