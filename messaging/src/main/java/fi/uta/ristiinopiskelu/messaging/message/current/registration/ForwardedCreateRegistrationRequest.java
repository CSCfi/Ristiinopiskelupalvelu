package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ForwardedCreateRegistrationRequest extends CreateRegistrationRequest {

    private String registrationRequestId;

    public static ForwardedCreateRegistrationRequest from(CreateRegistrationRequest source) {
        if (source == null) {
            return null;
        }

        ForwardedCreateRegistrationRequest target = new ForwardedCreateRegistrationRequest();
        target.setSendingOrganisationTkCode(source.getSendingOrganisationTkCode());
        target.setReceivingOrganisationTkCode(source.getReceivingOrganisationTkCode());
        target.setStudent(source.getStudent());
        target.setNetworkIdentifier(source.getNetworkIdentifier());
        target.setNetworkDescription(source.getNetworkDescription());
        target.setSelections(source.getSelections());
        target.setEnrolmentDateTime(source.getEnrolmentDateTime());
        target.setWarnings(source.getWarnings());
        target.setDescriptions(source.getDescriptions());
        return target;
    }
}
