package fi.uta.ristiinopiskelu.messaging.message.v8.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Description;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudentWarning;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractPersonIdentifiableRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateRegistrationRequest extends AbstractPersonIdentifiableRequest {

    private String sendingOrganisationTkCode;
    private String receivingOrganisationTkCode;
    private ExtendedStudent student;
    private String networkIdentifier;
    private String networkDescription;
    private List<RegistrationSelection> selections;
    private OffsetDateTime enrolmentDateTime;

    @Deprecated(since = "9.0.28")
    private List<StudentWarning> warnings;

    private List<Description> descriptions;

    @JsonIgnore
    @Override
    public String getPersonIdentifier() {
        if(getStudent() != null) {
            return !StringUtils.isEmpty(getStudent().getPersonId()) ?
                    getStudent().getPersonId() : getStudent().getOid();
        }

        return null;
    }
}
