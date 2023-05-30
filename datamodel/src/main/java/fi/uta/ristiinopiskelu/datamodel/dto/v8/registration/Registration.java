package fi.uta.ristiinopiskelu.datamodel.dto.v8.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent;

import java.time.OffsetDateTime;
import java.util.List;

public class Registration {
    private String registrationRequestId;
    private String sendingOrganisationTkCode;
    private String receivingOrganisationTkCode;
    private String networkIdentifier;
    private String networkDescription;
    private OffsetDateTime sendDateTime;
    private OffsetDateTime enrolmentDateTime;
    private OffsetDateTime receivingDateTime;
    private RegistrationStatus status;
    private String statusInfo;
    private ExtendedStudent student;
    private List<RegistrationSelection> selections;
    private List<RegistrationSelection> selectionsReplies;

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

    public String getNetworkDescription() {
        return networkDescription;
    }

    public void setNetworkDescription(String networkDescription) {
        this.networkDescription = networkDescription;
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

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
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

    public List<RegistrationSelection> getSelectionsReplies() {
        return selectionsReplies;
    }

    public void setSelectionsReplies(List<RegistrationSelection> selectionsReplies) {
        this.selectionsReplies = selectionsReplies;
    }

    public String getRegistrationRequestId() {
        return registrationRequestId;
    }

    public void setRegistrationRequestId(String registrationRequestId) {
        this.registrationRequestId = registrationRequestId;
    }
}
