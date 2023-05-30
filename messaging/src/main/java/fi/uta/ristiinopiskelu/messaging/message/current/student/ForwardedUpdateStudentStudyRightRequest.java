package fi.uta.ristiinopiskelu.messaging.message.current.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStudyRight;

import java.util.List;

public class ForwardedUpdateStudentStudyRightRequest extends AbstractForwardedStudentMessage {
    private String studentRequestId;
    private String oid;
    private StudentStudyRight homeStudyRight;
    private List<StudyRightIdentifier> hostStudyRightIdentifiers;

    @Override
    public String getStudentRequestId() {
        return studentRequestId;
    }

    @Override
    public void setStudentRequestId(String studentRequestId) {
        this.studentRequestId = studentRequestId;
    }

    @Override
    public List<StudyRightIdentifier> getHostStudyRightIdentifiers() {
        return hostStudyRightIdentifiers;
    }

    @Override
    public void setHostStudyRightIdentifiers(List<StudyRightIdentifier> hostStudyRightIdentifiers) {
        this.hostStudyRightIdentifiers = hostStudyRightIdentifiers;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public StudentStudyRight getHomeStudyRight() {
        return homeStudyRight;
    }

    public void setHomeStudyRight(StudentStudyRight homeStudyRight) {
        this.homeStudyRight = homeStudyRight;
    }

}
