package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.ExtendedStudentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationStudentEntity {

    private String homeStudentNumber;
    private String hostStudentNumber;
    private StudyRightEntity hostStudyRight;
    private StudyRightEntity homeStudyRight;

    public static RegistrationStudentEntity from(ExtendedStudentEntity student) {
        if(student == null) {
            return null;
        }

        return RegistrationStudentEntity.builder()
            .homeStudentNumber(student.getHomeStudentNumber())
            .hostStudentNumber(student.getHostStudentNumber())
            .hostStudyRight(StudyRightEntity.fromStudyRight(student.getHostStudyRight()))
            .homeStudyRight(StudyRightEntity.fromStudyRight(student.getHomeStudyRight()))
            .build();
    }

    public static RegistrationStudentEntity from(ExtendedStudent source) {
        if (source == null) {
            return null;
        }

        RegistrationStudentEntity target = new RegistrationStudentEntity();
        target.setHomeStudentNumber(source.getHomeStudentNumber());
        target.setHostStudentNumber(source.getHostStudentNumber());
        target.setHomeStudyRight(StudyRightEntity.fromStudyRight(source.getHomeStudyRight()));
        target.setHostStudyRight(StudyRightEntity.fromStudyRight(source.getHostStudyRight()));
        return target;
    }
}
