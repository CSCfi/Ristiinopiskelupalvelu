package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudyRightEntity {

    private StudyRightIdentifier identifiers;
    private StudyRightStatus studyRightStatus;
    private StudyRightType studyRightType;

    @Builder
    public StudyRightEntity(StudyRightIdentifier identifiers, StudyRightStatus studyRightStatus, StudyRightType studyRightType) {
        this.identifiers = identifiers;
        this.studyRightStatus = studyRightStatus;
        this.studyRightType = studyRightType;
    }

    public static StudyRightEntity fromStudyRight(StudyRight studyRight) {
        if(studyRight == null) {
            return null;
        }

        return StudyRightEntity.builder()
            .identifiers(studyRight.getIdentifiers())
            .studyRightStatus(studyRight.getStudyRightStatus())
            .studyRightType(studyRight.getStudyRightType())
            .build();
    }
}
