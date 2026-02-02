package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.StudyRightType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStudyRight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=true)
@AllArgsConstructor
@NoArgsConstructor
public class StudentStudyRightEntity extends StudyRightEntity {

    private Boolean eligibleForNetworkStudies;

    public StudentStudyRightEntity(StudyRightIdentifier identifiers, StudyRightStatus studyRightStatus, StudyRightType studyRightType,
                                   Boolean eligibleForNetworkStudies) {
        super(identifiers, studyRightStatus, studyRightType);
        this.eligibleForNetworkStudies = eligibleForNetworkStudies;
    }

    public static StudentStudyRightEntity fromStudentStudyRight(StudentStudyRight studentStudyRight) {
        if(studentStudyRight == null) {
            return null;
        }

        return new StudentStudyRightEntity(studentStudyRight.getIdentifiers(), studentStudyRight.getStudyRightStatus(),
                studentStudyRight.getStudyRightType(), studentStudyRight.getEligibleForNetworkStudies());
    }
}
