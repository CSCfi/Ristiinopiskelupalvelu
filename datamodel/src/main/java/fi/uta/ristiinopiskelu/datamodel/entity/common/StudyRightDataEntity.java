package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyRightEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.StudyRightTypeEntity;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class StudyRightDataEntity {

    private StudyRightIdentifierEntity identifiers;
    private StudyRightStatusEntity studyRightStatus;
    private StudyRightTypeEntity studyRightType;
    private List<KeywordEntity> keywords;

    @Builder
    public StudyRightDataEntity(StudyRightIdentifierEntity identifiers, StudyRightStatusEntity studyRightStatus, StudyRightTypeEntity studyRightType,
                      List<KeywordEntity> keywords) {
        this.identifiers = identifiers;
        this.studyRightStatus = studyRightStatus;
        this.studyRightType = studyRightType;
        this.keywords = keywords;
    }

    public static StudyRightDataEntity from(StudyRightDataEntity studyRight) {
        if(studyRight == null) {
            return null;
        }

        return StudyRightDataEntity.builder()
            .identifiers(studyRight.getIdentifiers())
            .studyRightStatus(studyRight.getStudyRightStatus())
            .studyRightType(studyRight.getStudyRightType())
            .build();
    }

    public static StudyRightDataEntity from(StudyRightEntity studyRight) {
        if (studyRight == null) {
            return null;
        }

        return StudyRightDataEntity.builder()
            .identifiers(studyRight.getIdentifiers())
            .studyRightStatus(studyRight.getStudyRightStatus())
            .studyRightType(studyRight.getStudyRightType())
            .build();
    }
}
