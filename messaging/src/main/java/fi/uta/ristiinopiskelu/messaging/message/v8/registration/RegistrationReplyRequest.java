package fi.uta.ristiinopiskelu.messaging.message.v8.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class RegistrationReplyRequest extends AbstractRequest {

    private String registrationRequestId;
    private StudyRight hostStudyRight;
    private String hostStudentEppn;
    private String hostStudentNumber;
    private List<RegistrationSelection> selections;
    private RegistrationStatus status;
    private String statusInfo;
    private LocalisedString rejectionReason;

    public String getRegistrationRequestId() {
        return registrationRequestId;
    }

    public void setRegistrationRequestId(String registrationRequestId) {
        this.registrationRequestId = registrationRequestId;
    }

    public StudyRight getHostStudyRight() {
        return hostStudyRight;
    }

    public void setHostStudyRight(StudyRight hostStudyRight) {
        this.hostStudyRight = hostStudyRight;
    }

    public String getHostStudentEppn() {
        return hostStudentEppn;
    }

    public void setHostStudentEppn(String hostStudentEppn) {
        this.hostStudentEppn = hostStudentEppn;
    }

    public String getHostStudentNumber() {
        return hostStudentNumber;
    }

    public void setHostStudentNumber(String hostStudentNumber) {
        this.hostStudentNumber = hostStudentNumber;
    }

    public List<RegistrationSelection> getSelections() {
        return selections;
    }

    public void setSelections(List<RegistrationSelection> selections) {
        this.selections = selections;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
    }

    public LocalisedString getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(LocalisedString rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @Override
    public String toString() {
        return "RegistrationReply{" +
                "registrationRequestId='" + registrationRequestId + '\'' +
                ", hostStudyRight=" + hostStudyRight +
                ", hostEppn='" + hostStudentEppn + '\'' +
                ", hostStudentNumber='" + hostStudentNumber + '\'' +
                ", status=" + status +
                ", statusInfo='" + statusInfo + '\'' +
                '}';
    }
}
