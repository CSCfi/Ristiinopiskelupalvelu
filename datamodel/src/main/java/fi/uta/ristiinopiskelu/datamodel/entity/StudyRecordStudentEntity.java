package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.StudyRecordStudentDataEntity;
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
    private StudyRightIdentifierEntity homeStudyRightIdentifier;
    private StudyRightIdentifierEntity hostStudyRightIdentifier;

    public static StudyRecordStudentEntity from(StudyRecordStudentDataEntity student) {
        return StudyRecordStudentEntity.builder()
                .homeStudentNumber(student.getHomeStudentNumber())
                .hostStudentNumber(student.getHostStudentNumber())
                .homeStudyRightIdentifier(student.getHomeStudyRightIdentifier())
                .hostStudyRightIdentifier(student.getHostStudyRightIdentifier())
                .build();
    }

    public static StudyRecordStudentEntity from(StudyRecordStudent source) {
        if (source == null) {
            return null;
        }

        StudyRecordStudentEntity target = new StudyRecordStudentEntity();
        target.setHomeStudentNumber(source.getHomeStudentNumber());
        target.setHostStudentNumber(source.getHostStudentNumber());
        target.setHomeStudyRightIdentifier(StudyRightIdentifierEntity.from(source.getHomeStudyRightIdentifier()));
        target.setHostStudyRightIdentifier(StudyRightIdentifierEntity.from(source.getHostStudyRightIdentifier()));
        return target;
    }
}
