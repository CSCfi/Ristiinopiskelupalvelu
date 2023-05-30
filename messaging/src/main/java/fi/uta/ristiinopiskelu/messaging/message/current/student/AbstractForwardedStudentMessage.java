package fi.uta.ristiinopiskelu.messaging.message.current.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

import java.util.List;

public abstract class AbstractForwardedStudentMessage extends AbstractRequest {
    public abstract void setHostStudyRightIdentifiers(List<StudyRightIdentifier> collect);
    public abstract List<StudyRightIdentifier> getHostStudyRightIdentifiers();
    public abstract String getStudentRequestId();
    public abstract void setStudentRequestId(String studentRequestId);
}
