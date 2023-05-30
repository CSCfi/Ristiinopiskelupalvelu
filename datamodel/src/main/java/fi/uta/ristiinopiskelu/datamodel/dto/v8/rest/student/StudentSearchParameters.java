package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.student;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Student;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.PageableSearchParameters;

import java.time.OffsetDateTime;
import java.util.List;

public class StudentSearchParameters extends PageableSearchParameters<Student> {

    private String studentOid;
    private String studentPersonId;
    private String studentHomeEppn;
    private List<String> networkIdentifiers;
    private OffsetDateTime sendDateTimeStart;
    private OffsetDateTime sendDateTimeEnd;
    private RegistrationStatus registrationStatus;

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

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }
}
