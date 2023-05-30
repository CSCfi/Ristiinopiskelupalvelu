package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;

public class StudyRecordStudent extends Student {

    private StudyRightIdentifier homeStudyRightIdentifier;
    private StudyRightIdentifier hostStudyRightIdentifier;

    public StudyRightIdentifier getHostStudyRightIdentifier() { return hostStudyRightIdentifier; }

    public void setHostStudyRightIdentifier(StudyRightIdentifier hostStudyRightIdentifier) { this.hostStudyRightIdentifier = hostStudyRightIdentifier; }

    public StudyRightIdentifier getHomeStudyRightIdentifier() { return homeStudyRightIdentifier; }

    public void setHomeStudyRightIdentifier(StudyRightIdentifier homeStudyRightIdentifier) { this.homeStudyRightIdentifier = homeStudyRightIdentifier; }

    public StudyRecordStudent (){};

    public StudyRecordStudent(ExtendedStudent student){
        super(student);
        homeStudyRightIdentifier = student.getHomeStudyRight().getIdentifiers();
        hostStudyRightIdentifier = student.getHostStudyRight().getIdentifiers();

    }

}
