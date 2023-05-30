package fi.uta.ristiinopiskelu.messaging.message.v8.student;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

import java.util.List;

public abstract class AbstractForwardedStudentMessage extends AbstractRequest {
    public abstract void setHostStudyRightIdentifiers(List<StudyRightIdentifier> collect);
    public abstract List<StudyRightIdentifier> getHostStudyRightIdentifiers();
    public abstract String getStudentRequestId();
    public abstract void setStudentRequestId(String studentRequestId);
}
