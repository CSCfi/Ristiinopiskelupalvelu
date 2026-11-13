package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightDataEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightStatusValueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.StudyRightTypeEntity;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudyRightEntity {

    private StudyRightIdentifierEntity identifiers;
    private StudyRightStatusEntity studyRightStatus;
    private StudyRightTypeEntity studyRightType;

    @Builder
    public StudyRightEntity(StudyRightIdentifierEntity identifiers, StudyRightStatusEntity studyRightStatus, StudyRightTypeEntity studyRightType) {
        this.identifiers = identifiers;
        this.studyRightStatus = studyRightStatus;
        this.studyRightType = studyRightType;
    }

    public static StudyRightEntity fromStudyRight(StudyRightDataEntity studyRight) {
        if(studyRight == null) {
            return null;
        }

        return StudyRightEntity.builder()
            .identifiers(studyRight.getIdentifiers())
            .studyRightStatus(studyRight.getStudyRightStatus())
            .studyRightType(studyRight.getStudyRightType())
            .build();
    }

    public static StudyRightEntity fromStudyRight(StudyRight studyRight) {
        if (studyRight == null) {
            return null;
        }

        return StudyRightEntity.builder()
            .identifiers(studyRight.getIdentifiers() == null ? null :
                new StudyRightIdentifierEntity(studyRight.getIdentifiers().getOrganisationTkCodeReference(), studyRight.getIdentifiers().getStudyRightId()))
            .studyRightStatus(studyRight.getStudyRightStatus() == null ? null :
                new StudyRightStatusEntity(
                    studyRight.getStudyRightStatus().getStudyRightStatusValue() == null ? null :
                        StudyRightStatusValueEntity.valueOf(studyRight.getStudyRightStatus().getStudyRightStatusValue().name()),
                    studyRight.getStudyRightStatus().getStartDate(),
                    studyRight.getStudyRightStatus().getEndDate()))
            .studyRightType(studyRight.getStudyRightType() == null ? null : StudyRightTypeEntity.valueOf(studyRight.getStudyRightType().name()))
            .build();
    }
}
