package fi.uta.ristiinopiskelu.messaging.message.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public class UpdateStudyModuleRequest extends AbstractRequest {
    
    private StudyModuleWriteDTO studyModule;

    public StudyModuleWriteDTO getStudyModule() {
        return studyModule;
    }

    public void setStudyModule(StudyModuleWriteDTO studyModule) {
        this.studyModule = studyModule;
    }
}
