package fi.uta.ristiinopiskelu.messaging.message.v8.studymodule;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyModule;

public class UpdateStudyModuleRequest extends AbstractRequest {
    
    private StudyModule studyModule;

    public StudyModule getStudyModule() {
        return studyModule;
    }

    public void setStudyModule(StudyModule studyModule) {
        this.studyModule = studyModule;
    }
}
