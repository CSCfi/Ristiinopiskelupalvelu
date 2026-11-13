package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.Student;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationStudentEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordStudentEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(name = "StudyRecordStudentV9", title = "StudyRecordStudentV9")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class StudyRecordStudent extends Student {

    private StudyRightIdentifier homeStudyRightIdentifier;
    private StudyRightIdentifier hostStudyRightIdentifier;

    public StudyRecordStudent(ExtendedStudent student) {
        super(student);
        homeStudyRightIdentifier = student.getHomeStudyRight().getIdentifiers();
        hostStudyRightIdentifier = student.getHostStudyRight().getIdentifiers();
    }

    public StudyRecordStudent(RegistrationStudentEntity registrationStudent) {
        setHomeStudentNumber(registrationStudent.getHomeStudentNumber());
        setHostStudentNumber(registrationStudent.getHostStudentNumber());
        setHomeStudyRightIdentifier(registrationStudent.getHomeStudyRight() != null
            ? toDto(registrationStudent.getHomeStudyRight().getIdentifiers())
            : null);
        setHostStudyRightIdentifier(registrationStudent.getHostStudyRight() != null
            ? toDto(registrationStudent.getHostStudyRight().getIdentifiers())
            : null);
    }

    @Builder
    public StudyRecordStudent(String homeEppn, String hostEppn, String firstNames, String surName, String givenName,
                              String oid, String personId, String homeStudentNumber, String hostStudentNumber,
                              StudyRightIdentifier homeStudyRightIdentifier, StudyRightIdentifier hostStudyRightIdentifier) {
        super(homeEppn, hostEppn, firstNames, surName, givenName, oid, personId, homeStudentNumber, hostStudentNumber);
        this.homeStudyRightIdentifier = homeStudyRightIdentifier;
        this.hostStudyRightIdentifier = hostStudyRightIdentifier;
    }

    public static StudyRecordStudent from(StudyRecordStudentEntity studyRecordStudent) {
        if(studyRecordStudent == null) {
            return null;
        }

        return StudyRecordStudent.builder()
            .homeStudentNumber(studyRecordStudent.getHomeStudentNumber())
            .hostStudentNumber(studyRecordStudent.getHostStudentNumber())
            .homeStudyRightIdentifier(toDto(studyRecordStudent.getHomeStudyRightIdentifier()))
            .hostStudyRightIdentifier(toDto(studyRecordStudent.getHostStudyRightIdentifier()))
            .build();
    }

    private static StudyRightIdentifier toDto(fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity identifier) {
        if (identifier == null) {
            return null;
        }

        return new StudyRightIdentifier(identifier.getOrganisationTkCodeReference(), identifier.getStudyRightId());
    }
}
