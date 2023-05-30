package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecord;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class StudyRecordSearchParameters extends PageableSearchParameters<StudyRecord> {

    private String studentOid;
    private String studentPersonId;
    private String studentHomeEppn;
    private List<String> networkIdentifiers;
    private OffsetDateTime sendDateTimeStart;
    private OffsetDateTime sendDateTimeEnd;
    private StudyRecordStatus studyRecordStatus;

    public StudyRecordSearchParameters() {
        
    }

    public StudyRecordSearchParameters(String studentOid, String studentPersonId, String studentHomeEppn,
                                        List<String> networkIdentifiers, OffsetDateTime sendDateTimeStart,
                                        OffsetDateTime sendDateTimeEnd, StudyRecordStatus studyRecordStatus) {
        this.studentOid = studentOid;
        this.studentPersonId = studentPersonId;
        this.studentHomeEppn = studentHomeEppn;
        this.networkIdentifiers = networkIdentifiers;
        this.sendDateTimeStart = sendDateTimeStart;
        this.sendDateTimeEnd = sendDateTimeEnd;
        this.studyRecordStatus = studyRecordStatus;
    }

    public String getStudentOid() {
        return studentOid;
    }

    public void setStudentOid(String studentOid) {
        this.studentOid = studentOid;
    }

    public String getStudentPersonId() {
        return studentPersonId;
    }

    public void setStudentPersonId(String studentPersonId) {
        this.studentPersonId = studentPersonId;
    }

    public String getStudentHomeEppn() {
        return studentHomeEppn;
    }

    public void setStudentHomeEppn(String studentHomeEppn) {
        this.studentHomeEppn = studentHomeEppn;
    }

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }

    public OffsetDateTime getSendDateTimeStart() {
        return sendDateTimeStart;
    }

    public void setSendDateTimeStart(OffsetDateTime sendDateTimeStart) {
        this.sendDateTimeStart = sendDateTimeStart;
    }

    public OffsetDateTime getSendDateTimeEnd() {
        return sendDateTimeEnd;
    }

    public void setSendDateTimeEnd(OffsetDateTime sendDateTimeEnd) {
        this.sendDateTimeEnd = sendDateTimeEnd;
    }

    public StudyRecordStatus getStudyRecordStatus() {
        return studyRecordStatus;
    }

    public void setStudyRecordStatus(StudyRecordStatus studyRecordStatus) {
        this.studyRecordStatus = studyRecordStatus;
    }
}
