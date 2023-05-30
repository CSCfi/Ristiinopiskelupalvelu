package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.student;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight;

import java.util.List;

public class StudentSearchResult {

    private Student student;
    private List<StudyRight> hostStudyRights;
    private List<StudyRight> homeStudyRights;

    public StudentSearchResult(Student student, List<StudyRight> hostStudyRights, List<StudyRight> homeStudyRights) {
        this.student = student;
        this.hostStudyRights = hostStudyRights;
        this.homeStudyRights = homeStudyRights;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public List<StudyRight> getHostStudyRights() {
        return hostStudyRights;
    }

    public void setHostStudyRights(List<StudyRight> hostStudyRights) {
        this.hostStudyRights = hostStudyRights;
    }

    public List<StudyRight> getHomeStudyRights() {
        return homeStudyRights;
    }

    public void setHomeStudyRights(List<StudyRight> homeStudyRights) {
        this.homeStudyRights = homeStudyRights;
    }
}
