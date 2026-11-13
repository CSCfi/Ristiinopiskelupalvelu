package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationStudentEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyRightIdentifierEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.ExtendedStudentEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.student.StudentDataEntity;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StudyRecordStudentDataEntity extends StudentDataEntity {

    private StudyRightIdentifierEntity homeStudyRightIdentifier;
    private StudyRightIdentifierEntity hostStudyRightIdentifier;

    public StudyRecordStudentDataEntity(ExtendedStudentEntity student) {
        super(student);
        homeStudyRightIdentifier = student.getHomeStudyRight().getIdentifiers();
        hostStudyRightIdentifier = student.getHostStudyRight().getIdentifiers();
    }

    public StudyRecordStudentDataEntity(RegistrationStudentEntity registrationStudent) {
        setHomeStudentNumber(registrationStudent.getHomeStudentNumber());
        setHostStudentNumber(registrationStudent.getHostStudentNumber());
        setHomeStudyRightIdentifier(registrationStudent.getHomeStudyRight().getIdentifiers());
        setHostStudyRightIdentifier(registrationStudent.getHostStudyRight().getIdentifiers());
    }

    @Builder
    public StudyRecordStudentDataEntity(String homeEppn, String hostEppn, String firstNames, String surName, String givenName,
                              String oid, String personId, String homeStudentNumber, String hostStudentNumber,
                              StudyRightIdentifierEntity homeStudyRightIdentifier, StudyRightIdentifierEntity hostStudyRightIdentifier) {
        super(homeEppn, hostEppn, firstNames, surName, givenName, oid, personId, homeStudentNumber, hostStudentNumber);
        this.homeStudyRightIdentifier = homeStudyRightIdentifier;
        this.hostStudyRightIdentifier = hostStudyRightIdentifier;
    }

    public static StudyRecordStudentDataEntity from(StudyRecordStudentDataEntity studyRecordStudent) {
        if(studyRecordStudent == null) {
            return null;
        }

        return StudyRecordStudentDataEntity.builder()
            .homeStudentNumber(studyRecordStudent.getHomeStudentNumber())
            .hostStudentNumber(studyRecordStudent.getHostStudentNumber())
            .homeStudyRightIdentifier(studyRecordStudent.getHomeStudyRightIdentifier())
            .hostStudyRightIdentifier(studyRecordStudent.getHostStudyRightIdentifier())
            .build();
    }
}
