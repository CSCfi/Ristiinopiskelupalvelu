package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Person;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

public class CompletedCredit {
    private String educationInstitution;
    private String completedCreditIdentifier;
    private CompletedCreditTarget completedCreditTarget;
    private List<CompletedCreditAssociation> completedCreditAssociations;
    private List<String> assessmentItemRealisationsOfMethodOfCompletion;
    private LocalisedString completedCreditName;
    private LocalisedString completedCreditObjective;
    private LocalisedString completedCreditContent;
    private CompletedCreditStatus completedCreditStatus;
    private Double scope;
    private CompletedCreditAssessment assessment;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate completionDate;

    private List<String> languagesOfCompletion;
    private List<Person> acceptors;
    private StudyRecordOrganisation organisationResponsibleForCompletion;
    private CompletedCreditTransaction transaction;

    @Field(type = FieldType.Integer)
    private CompletedCreditType type;

    @Field(type = FieldType.Integer)
    private MinEduGuidanceArea minEduGuidanceArea;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate assessmentDate;

    public String getEducationInstitution() {
        return educationInstitution;
    }

    public void setEducationInstitution(String educationInstitution) {
        this.educationInstitution = educationInstitution;
    }

    public String getCompletedCreditIdentifier() {
        return completedCreditIdentifier;
    }

    public void setCompletedCreditIdentifier(String completedCreditIdentifier) {
        this.completedCreditIdentifier = completedCreditIdentifier;
    }

    public CompletedCreditTarget getCompletedCreditTarget() {
        return completedCreditTarget;
    }

    public void setCompletedCreditTarget(CompletedCreditTarget completedCreditTarget) {
        this.completedCreditTarget = completedCreditTarget;
    }

    public List<CompletedCreditAssociation> getCompletedCreditAssociations() {
        return completedCreditAssociations;
    }

    public void setCompletedCreditAssociations(List<CompletedCreditAssociation> completedCreditAssociations) {
        this.completedCreditAssociations = completedCreditAssociations;
    }

    public List<String> getAssessmentItemRealisationsOfMethodOfCompletion() {
        return assessmentItemRealisationsOfMethodOfCompletion;
    }

    public void setAssessmentItemRealisationsOfMethodOfCompletion(List<String> assessmentItemRealisationsOfMethodOfCompletion) {
        this.assessmentItemRealisationsOfMethodOfCompletion = assessmentItemRealisationsOfMethodOfCompletion;
    }

    public LocalisedString getCompletedCreditName() {
        return completedCreditName;
    }

    public void setCompletedCreditName(LocalisedString completedCreditName) {
        this.completedCreditName = completedCreditName;
    }

    public LocalisedString getCompletedCreditObjective() {
        return completedCreditObjective;
    }

    public void setCompletedCreditObjective(LocalisedString completedCreditObjective) {
        this.completedCreditObjective = completedCreditObjective;
    }

    public LocalisedString getCompletedCreditContent() {
        return completedCreditContent;
    }

    public void setCompletedCreditContent(LocalisedString completedCreditContent) {
        this.completedCreditContent = completedCreditContent;
    }

    public CompletedCreditStatus getCompletedCreditStatus() {
        return completedCreditStatus;
    }

    public void setCompletedCreditStatus(CompletedCreditStatus completedCreditStatus) {
        this.completedCreditStatus = completedCreditStatus;
    }

    public Double getScope() {
        return scope;
    }

    public void setScope(Double scope) {
        this.scope = scope;
    }

    public CompletedCreditAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(CompletedCreditAssessment assessment) {
        this.assessment = assessment;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public List<String> getLanguagesOfCompletion() {
        return languagesOfCompletion;
    }

    public void setLanguagesOfCompletion(List<String> languagesOfCompletion) {
        this.languagesOfCompletion = languagesOfCompletion;
    }

    public List<Person> getAcceptors() {
        return acceptors;
    }

    public void setAcceptors(List<Person> acceptors) {
        this.acceptors = acceptors;
    }

    public StudyRecordOrganisation getOrganisationResponsibleForCompletion() {
        return organisationResponsibleForCompletion;
    }

    public void setOrganisationResponsibleForCompletion(StudyRecordOrganisation organisationResponsibleForCompletion) {
        this.organisationResponsibleForCompletion = organisationResponsibleForCompletion;
    }

    public CompletedCreditTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(CompletedCreditTransaction transaction) {
        this.transaction = transaction;
    }

    public CompletedCreditType getType() {
        return type;
    }

    public void setType(CompletedCreditType type) {
        this.type = type;
    }

    public MinEduGuidanceArea getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(MinEduGuidanceArea minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }
}
