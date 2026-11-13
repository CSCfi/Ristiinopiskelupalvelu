package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
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
        this.setStudent(ExtendedStudent.from(entity.getStudent()));
        //Remove personId from reply message
        this.getStudent().setPersonId(null);
        this.setSelections(CreateRegistrationSelection.fromReplyEntity(entity.getSelectionsReplies()));
        this.setStatus(RegistrationStatus.from(entity.getStatus()));
        this.setRejectionReason(LocalisedString.from(entity.getRejectionReason()));
    }
}
