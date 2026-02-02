package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Description;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractPersonIdentifiableRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @Override
    public String toString() {
        return "RegistrationRequest{" +
                "sendingOrganisationTkCode='" + sendingOrganisationTkCode + '\'' +
                ", receivingOrganisationTkCode='" + receivingOrganisationTkCode + '\'' +
                ", student=" + student +
                ", networkIdentifier='" + networkIdentifier + '\'' +
                "} " + super.toString();
    }

    @JsonIgnore
    @Override
    public String getPersonId() {
        return getStudent() != null ? getStudent().getPersonId() : null;
    }

    @JsonIgnore
    @Override
    public String getPersonOid() {
        return getStudent() != null ? getStudent().getOid() : null;
    }
}
