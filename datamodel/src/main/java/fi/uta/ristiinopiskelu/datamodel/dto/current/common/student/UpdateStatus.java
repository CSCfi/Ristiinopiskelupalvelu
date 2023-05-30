package fi.uta.ristiinopiskelu.datamodel.dto.current.common.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;

public class UpdateStatus {
    private String organisationId;
    private StudentStatus status;
    private LocalisedString rejectionReason;

    public UpdateStatus() {}

    public UpdateStatus(String organisationId, StudentStatus status, LocalisedString rejectionReason) {
        this.organisationId = organisationId;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public UpdateStatus(String organisationId, StudentStatus status) {
        this.organisationId = organisationId;
        this.status = status;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
