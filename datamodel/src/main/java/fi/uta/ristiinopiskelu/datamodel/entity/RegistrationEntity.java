package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Description;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "rekisteroinnit", createIndex = false)
public class RegistrationEntity extends GenericEntity implements Serializable {
    private String sendingOrganisationTkCode;
    private String receivingOrganisationTkCode;
    private String networkIdentifier;
    private String networkDescription;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrolmentDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime receivingDateTime;
    
    private RegistrationStatus status;
    private String statusInfo;
    private ExtendedStudent student;
    private List<RegistrationSelection> selections;
    private List<RegistrationSelection> selectionsReplies;
    private List<StudentWarning> warnings;
    private List<Description> descriptions;
    private LocalisedString rejectionReason;

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

    public String getNetworkIdentifier() {
        return networkIdentifier;
    }

    public void setNetworkIdentifier(String networkIdentifier) {
        this.networkIdentifier = networkIdentifier;
    }

    public OffsetDateTime getSendDateTime() {
        return sendDateTime;
    }

    public void setSendDateTime(OffsetDateTime sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    public OffsetDateTime getEnrolmentDateTime() {
        return enrolmentDateTime;
    }

    public void setEnrolmentDateTime(OffsetDateTime enrolmentDateTime) {
        this.enrolmentDateTime = enrolmentDateTime;
    }

    public OffsetDateTime getReceivingDateTime() {
        return receivingDateTime;
    }

    public void setReceivingDateTime(OffsetDateTime receivingDateTime) {
        this.receivingDateTime = receivingDateTime;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public ExtendedStudent getStudent() {
        return student;
    }

    public void setStudent(ExtendedStudent student) {
        this.student = student;
    }

    public List<RegistrationSelection> getSelections() {
        return selections;
    }

    public void setSelections(List<RegistrationSelection> selections) {
        this.selections = selections;
    }

    public String getNetworkDescription() {
        return networkDescription;
    }

    public void setNetworkDescription(String networkDescription) {
        this.networkDescription = networkDescription;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
    }

    public List<RegistrationSelection> getSelectionsReplies() {
        return selectionsReplies;
    }

    public void setSelectionsReplies(List<RegistrationSelection> selectionsReplies) {
        this.selectionsReplies = selectionsReplies;
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

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
