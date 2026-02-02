package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ForwardedCreateRegistrationRequest extends CreateRegistrationRequest {

    private String registrationRequestId;
}
