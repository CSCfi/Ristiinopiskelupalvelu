package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.StudyRightTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.StudentStudyRightDataEntity;
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

    public StudentStudyRightEntity(StudyRightIdentifierEntity identifiers, StudyRightStatusEntity studyRightStatus, StudyRightTypeEntity studyRightType,
                                   Boolean eligibleForNetworkStudies) {
        super(identifiers, studyRightStatus, studyRightType);
        this.eligibleForNetworkStudies = eligibleForNetworkStudies;
    }

    public static StudentStudyRightEntity fromStudentStudyRight(StudentStudyRightDataEntity studentStudyRight) {
        if(studentStudyRight == null) {
            return null;
        }

        return new StudentStudyRightEntity(studentStudyRight.getIdentifiers(), studentStudyRight.getStudyRightStatus(),
                studentStudyRight.getStudyRightType(), studentStudyRight.getEligibleForNetworkStudies());
    }

    public static StudentStudyRightEntity from(StudentStudyRight source) {
        if (source == null) {
            return null;
        }

        StudentStudyRightEntity target = new StudentStudyRightEntity();
        target.setIdentifiers(source.getIdentifiers() == null ? null
            : new StudyRightIdentifierEntity(source.getIdentifiers().getOrganisationTkCodeReference(), source.getIdentifiers().getStudyRightId()));
        target.setStudyRightStatus(source.getStudyRightStatus() == null ? null
            : new StudyRightStatusEntity(
            source.getStudyRightStatus().getStudyRightStatusValue() == null ? null
                : fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightStatusValueEntity.fromValue(source.getStudyRightStatus().getStudyRightStatusValue().getCode()),
            source.getStudyRightStatus().getStartDate(),
            source.getStudyRightStatus().getEndDate()
        ));
        target.setStudyRightType(source.getStudyRightType() == null ? null
            : StudyRightTypeEntity.fromValue(source.getStudyRightType().getCode()));
        target.setEligibleForNetworkStudies(source.getEligibleForNetworkStudies());
        return target;
    }
}
