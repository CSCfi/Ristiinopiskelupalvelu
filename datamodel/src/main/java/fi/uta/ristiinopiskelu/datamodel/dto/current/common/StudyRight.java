package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRightEntity;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class StudyRight implements Serializable {

    private StudyRightIdentifier identifiers;
    private StudyRightStatus studyRightStatus;
    private StudyRightType studyRightType;
    private List<Keyword> keywords;

    @Builder
    public StudyRight(StudyRightIdentifier identifiers, StudyRightStatus studyRightStatus, StudyRightType studyRightType,
                      List<Keyword> keywords) {
        this.identifiers = identifiers;
        this.studyRightStatus = studyRightStatus;
        this.studyRightType = studyRightType;
        this.keywords = keywords;
    }

    public static StudyRight from(StudyRightEntity studyRight) {
        if(studyRight == null) {
            return null;
        }

        return StudyRight.builder()
            .identifiers(studyRight.getIdentifiers() != null
                ? new StudyRightIdentifier(
                    studyRight.getIdentifiers().getOrganisationTkCodeReference(),
                    studyRight.getIdentifiers().getStudyRightId())
                : null)
            .studyRightStatus(studyRight.getStudyRightStatus() != null
                ? new StudyRightStatus(
                    studyRight.getStudyRightStatus().getStudyRightStatusValue() != null
                        ? StudyRightStatusValue.valueOf(studyRight.getStudyRightStatus().getStudyRightStatusValue().name())
                        : null,
                    studyRight.getStudyRightStatus().getStartDate(),
                    studyRight.getStudyRightStatus().getEndDate())
                : null)
            .studyRightType(studyRight.getStudyRightType() != null
                ? StudyRightType.valueOf(studyRight.getStudyRightType().name())
                : null)
            .build();
    }
}
