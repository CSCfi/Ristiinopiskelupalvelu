package fi.uta.ristiinopiskelu.messaging.message.current.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Description;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractPersonIdentifiableRequest;

import java.time.OffsetDateTime;
import java.util.List;

public class CreateRegistrationRequest extends AbstractPersonIdentifiableRequest {

    private String sendingOrganisationTkCode;
    private String receivingOrganisationTkCode;
    private ExtendedStudent student;
    private String networkIdentifier;
    private String networkDescription;
    private List<RegistrationSelection> selections;
    private OffsetDateTime enrolmentDateTime;
    private List<StudentWarning> warnings;
    private List<Description> descriptions;

    public String getSendingOrganisationTkCode() {
        return sendingOrganisationTkCode;
    }

    public void setSendingOrganisationTkCode(String sendingOrganisationTkCode) {
        this.sendingOrganisationTkCode = sendingOrganisationTkCode;
    }

    public String getReceivingOrganisationTkCode() {
        return receivingOrganisationTkCode;
    }

    public void setReceivingOrganisationTkCode(String receivingOrganisationTkCode) {
        this.receivingOrganisationTkCode = receivingOrganisationTkCode;
    }

    public ExtendedStudent getStudent() {
        return student;
    }

    public void setStudent(ExtendedStudent student) {
        this.student = student;
    }

    public String getNetworkIdentifier() {
        return networkIdentifier;
    }

    public void setNetworkIdentifier(String networkIdentifier) {
        this.networkIdentifier = networkIdentifier;
    }

    public List<RegistrationSelection> getSelections() {
        return selections;
    }

    public void setSelections(List<RegistrationSelection> selections) {
        this.selections = selections;
    }

    public OffsetDateTime getEnrolmentDateTime() {
        return enrolmentDateTime;
    }

    public void setEnrolmentDateTime(OffsetDateTime enrolmentDateTime) {
        this.enrolmentDateTime = enrolmentDateTime;
    }

    public String getNetworkDescription() {
        return networkDescription;
    }

    public void setNetworkDescription(String networkDescription) {
        this.networkDescription = networkDescription;
    }

    public List<StudentWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<StudentWarning> warnings) {
        this.warnings = warnings;
    }

    public List<Description> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<Description> descriptions) {
        this.descriptions = descriptions;
    }

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
