package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.Transient;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyElementReferenceEntity {

    private String referenceIdentifier;
    private String referenceOrganizer;
    private StudyElementTypeEntity referenceType;
    private String referenceAssessmentItemId;
    private Boolean onlyEnrollableWithParent;

    public StudyElementReferenceEntity(String referenceIdentifier, String referenceOrganizer, StudyElementTypeEntity referenceType) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceType = referenceType;
    }

    public StudyElementReferenceEntity(String referenceIdentifier, String referenceOrganizer,
                                 StudyElementTypeEntity referenceType, String referenceAssessmentItemId) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceAssessmentItemId = referenceAssessmentItemId;
        this.referenceType = referenceType;
    }

    public StudyElementReferenceEntity(String referenceIdentifier, String referenceOrganizer, StudyElementTypeEntity referenceType,
                                 Boolean onlyEnrollableWithParent) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.referenceType = referenceType;
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }

    @Transient
    public static StudyElementReferenceEntity from(StudyElementEntity entity) {
        return new StudyElementReferenceEntity(entity.getStudyElementId(), entity.getOrganizingOrganisationId(), StudyElementTypeEntity.valueOf(entity.getType().name()));
    }

    public static StudyElementReferenceEntity from(StudyElementReference source) {
        if (source == null) {
            return null;
        }
        StudyElementReferenceEntity target = new StudyElementReferenceEntity();
        target.setReferenceIdentifier(source.getReferenceIdentifier());
        target.setReferenceOrganizer(source.getReferenceOrganizer());
        target.setReferenceType(StudyElementTypeEntity.from(source.getReferenceType()));
        target.setReferenceAssessmentItemId(source.getReferenceAssessmentItemId());
        target.setOnlyEnrollableWithParent(source.getOnlyEnrollableWithParent());
        return target;
    }

    public static List<StudyElementReferenceEntity> from(List<StudyElementReference> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
            .map(StudyElementReferenceEntity::from)
            .toList();
    }
}
