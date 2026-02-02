package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyRecordStudentEntity {

    private String homeStudentNumber;
    private String hostStudentNumber;
    private StudyRightIdentifier homeStudyRightIdentifier;
    private StudyRightIdentifier hostStudyRightIdentifier;

    public static StudyRecordStudentEntity from(StudyRecordStudent student) {
        return StudyRecordStudentEntity.builder()
                .homeStudentNumber(student.getHomeStudentNumber())
                .hostStudentNumber(student.getHostStudentNumber())
                .homeStudyRightIdentifier(student.getHomeStudyRightIdentifier())
                .hostStudyRightIdentifier(student.getHostStudyRightIdentifier())
                .build();
    }
}
