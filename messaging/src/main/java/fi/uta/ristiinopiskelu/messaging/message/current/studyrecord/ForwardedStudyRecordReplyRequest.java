package fi.uta.ristiinopiskelu.messaging.message.current.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class ForwardedStudyRecordReplyRequest extends ForwardedCreateStudyRecordRequest {

    private StudyRecordStatus status;
    private LocalisedString rejectionReason;

    public ForwardedStudyRecordReplyRequest(StudyRecordEntity entity) {
        setStudyRecordRequestId(entity.getId());
        setSendingOrganisation(entity.getSendingOrganisation());
        setReceivingOrganisation(entity.getReceivingOrganisation());
        setStudent(StudyRecordStudent.from(entity.getStudent()));
        //Remove personId from reply message
        getStudent().setPersonId(null);
        setCompletedCredits(entity.getCompletedCredits());
        setSendingTime(entity.getSendingTime());
        setRoutingType(entity.getRoutingType());
        setStatus(entity.getStatus());
        setRejectionReason(entity.getRejectionReason());
    }
}
