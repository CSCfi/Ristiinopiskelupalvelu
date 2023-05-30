package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.PageableSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;

import java.time.OffsetDateTime;
import java.util.List;

public class RegistrationSearchParameters extends PageableSearchParameters<RegistrationEntity> {

    private String studentOid;
    private String studentPersonId;
    private String studentHomeEppn;
    private List<String> networkIdentifiers;
    private OffsetDateTime sendDateTimeStart;
    private OffsetDateTime sendDateTimeEnd;
    private RegistrationStatus registrationStatus;

    public RegistrationSearchParameters() {
        
    }

    public RegistrationSearchParameters(String studentOid, String studentPersonId, String studentHomeEppn,
                                        List<String> networkIdentifiers, OffsetDateTime sendDateTimeStart,
                                        OffsetDateTime sendDateTimeEnd, RegistrationStatus registrationStatus) {
        this.studentOid = studentOid;
        this.studentPersonId = studentPersonId;
        this.studentHomeEppn = studentHomeEppn;
        this.networkIdentifiers = networkIdentifiers;
        this.sendDateTimeStart = sendDateTimeStart;
        this.sendDateTimeEnd = sendDateTimeEnd;
        this.registrationStatus = registrationStatus;
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

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }
}
