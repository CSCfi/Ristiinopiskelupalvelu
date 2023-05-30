package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;

import java.util.Objects;

public class StudyElementReference {
    private String referenceIdentifier;
    private String referenceOrganizer;
    private StudyElementType referenceType;
    private String referenceAssessmentItemId;
    private Boolean onlyEnrollableWithParent;

    public StudyElementReference() {
        
    }

    public StudyElementReference(String referenceIdentifier, String referenceOrganizer, StudyElementType referenceType) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceType = referenceType;
    }

    public StudyElementReference(String referenceIdentifier, String referenceOrganizer,
                                 StudyElementType referenceType, String referenceAssessmentItemId) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceAssessmentItemId = referenceAssessmentItemId;
        this.referenceType = referenceType;
    }

    public StudyElementReference(String referenceIdentifier, String referenceOrganizer, StudyElementType referenceType,
                                 Boolean onlyEnrollableWithParent) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceType = referenceType;
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }

    public StudyElementReference(String referenceIdentifier, String referenceOrganizer, StudyElementType referenceType,
                                 String referenceAssessmentItemId, Boolean onlyEnrollableWithParent) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceType = referenceType;
        this.referenceAssessmentItemId = referenceAssessmentItemId;
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }

    public String getReferenceIdentifier() {
        return referenceIdentifier;
    }

    public void setReferenceIdentifier(String referenceIdentifier) {
        this.referenceIdentifier = referenceIdentifier;
    }

    public String getReferenceOrganizer() {
        return referenceOrganizer;
    }

    public void setReferenceOrganizer(String referenceOrganizer) {
        this.referenceOrganizer = referenceOrganizer;
    }

    public StudyElementType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(StudyElementType referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceAssessmentItemId() {
        return referenceAssessmentItemId;
    }

    public void setReferenceAssessmentItemId(String referenceAssessmentItemId) {
        this.referenceAssessmentItemId = referenceAssessmentItemId;
    }

    public Boolean getOnlyEnrollableWithParent() {
        return onlyEnrollableWithParent;
    }

    public void setOnlyEnrollableWithParent(Boolean onlyEnrollableWithParent) {
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudyElementReference)) return false;
        StudyElementReference that = (StudyElementReference) o;
        return Objects.equals(referenceIdentifier, that.referenceIdentifier) &&
                Objects.equals(referenceOrganizer, that.referenceOrganizer) &&
                Objects.equals(referenceAssessmentItemId, that.referenceAssessmentItemId) &&
                referenceType == that.referenceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceIdentifier, referenceOrganizer, referenceType, referenceAssessmentItemId);
    }

    @Override
    public String toString() {
        return "StudyElementReference{" +
            "referenceIdentifier='" + referenceIdentifier + '\'' +
            ", referenceOrganizer='" + referenceOrganizer + '\'' +
            ", referenceType=" + referenceType +
            ", referenceAssessmentItemId='" + referenceAssessmentItemId + '\'' +
            ", onlyEnrollableWithParent=" + onlyEnrollableWithParent +
            '}';
    }

    @JsonIgnore
    public static StudyElementReference from(StudyElementEntity entity) {
        return new StudyElementReference(entity.getStudyElementId(), entity.getOrganizingOrganisationId(), StudyElementType.valueOf(entity.getType().name()));
    }

    @JsonIgnore
    public static StudyElementReference from(StudyElement studyElement, String organizingOrganisationId) {
        return new StudyElementReference(studyElement.getStudyElementId(), organizingOrganisationId, studyElement.getType());
    }
}
