package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier;

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
