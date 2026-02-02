package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

    @Builder
    public StudyRecordStudent(String homeEppn, String hostEppn, String firstNames, String surName, String givenName,
                              String oid, String personId, String homeStudentNumber, String hostStudentNumber,
                              StudyRightIdentifier homeStudyRightIdentifier, StudyRightIdentifier hostStudyRightIdentifier) {
        super(homeEppn, hostEppn, firstNames, surName, givenName, oid, personId, homeStudentNumber, hostStudentNumber);
        this.homeStudyRightIdentifier = homeStudyRightIdentifier;
        this.hostStudyRightIdentifier = hostStudyRightIdentifier;
    }
}
