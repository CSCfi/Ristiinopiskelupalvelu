package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
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

    public static RegistrationStudentEntity from(ExtendedStudent student) {
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
}
