package fi.uta.ristiinopiskelu.handler.validator.studyrecord;

import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.StudyRecordReplyRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StudyRecordStatusValidator implements RequestValidator<StudyRecordReplyRequest> {

    @Override
    public void validateRequest(StudyRecordReplyRequest reply, String organisationId) {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }
        
        if(StringUtils.isEmpty(reply.getStudyRecordRequestId())) {
            throw new InvalidMessageBodyException("Received study record reply request -message without study record id.");
        }
    }
}
