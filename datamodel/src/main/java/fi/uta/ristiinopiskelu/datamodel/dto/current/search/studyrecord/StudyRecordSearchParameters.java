package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditTargetType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;

import java.time.OffsetDateTime;

public class StudyRecordSearchParameters extends PageableSearchParameters<StudyRecordEntity> {

    private String sendingOrganisation;
    private String receivingOrganisation;
    private CompletedCreditTargetType completedCreditTargetType;
    private String completedCreditTargetId;
    private String completedCreditName;
    private Language completedCreditNameLanguage = Language.FI;
    private GradeStatus gradeStatus;
    private OffsetDateTime completionStartDate;
    private OffsetDateTime completionEndDate;
    private MinEduGuidanceArea minEduGuidanceArea;
    private String organisationResponsibleForCompletionTkCode;

    public String getSendingOrganisation() {
        return sendingOrganisation;
    }

    public void setSendingOrganisation(String sendingOrganisation) {
        this.sendingOrganisation = sendingOrganisation;
    }

    public String getReceivingOrganisation() {
        return receivingOrganisation;
    }

    public void setReceivingOrganisation(String receivingOrganisation) {
        this.receivingOrganisation = receivingOrganisation;
    }

    public CompletedCreditTargetType getCompletedCreditTargetType() {
        return completedCreditTargetType;
    }

    public void setCompletedCreditTargetType(CompletedCreditTargetType completedCreditTargetType) {
        this.completedCreditTargetType = completedCreditTargetType;
    }

    public String getCompletedCreditTargetId() {
        return completedCreditTargetId;
    }

    public void setCompletedCreditTargetId(String completedCreditTargetId) {
        this.completedCreditTargetId = completedCreditTargetId;
    }

    public String getCompletedCreditName() {
        return completedCreditName;
    }

    public void setCompletedCreditName(String completedCreditName) {
        this.completedCreditName = completedCreditName;
    }

    public void setCompletedCreditNameLanguage(Language completedCreditNameLanguage) {
        this.completedCreditNameLanguage = completedCreditNameLanguage;
    }

    public Language getCompletedCreditNameLanguage() {
        return completedCreditNameLanguage;
    }

    public GradeStatus getGradeStatus() {
        return gradeStatus;
    }

    public void setGradeStatus(GradeStatus gradeStatus) {
        this.gradeStatus = gradeStatus;
    }

    public OffsetDateTime getCompletionStartDate() {
        return completionStartDate;
    }

    public void setCompletionStartDate(OffsetDateTime completionStartDate) {
        this.completionStartDate = completionStartDate;
    }

    public OffsetDateTime getCompletionEndDate() {
        return completionEndDate;
    }

    public void setCompletionEndDate(OffsetDateTime completionEndDate) {
        this.completionEndDate = completionEndDate;
    }

    public MinEduGuidanceArea getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(MinEduGuidanceArea minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    public String getOrganisationResponsibleForCompletionTkCode() {
        return organisationResponsibleForCompletionTkCode;
    }

    public void setOrganisationResponsibleForCompletionTkCode(String organisationResponsibleForCompletionTkCode) {
        this.organisationResponsibleForCompletionTkCode = organisationResponsibleForCompletionTkCode;
    }
}
