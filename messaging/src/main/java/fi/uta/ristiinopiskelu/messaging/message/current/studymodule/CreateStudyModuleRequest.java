package fi.uta.ristiinopiskelu.messaging.message.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

import java.util.List;

public class CreateStudyModuleRequest extends AbstractRequest {

    private List<StudyModuleWriteDTO> studyModules;

    public List<StudyModuleWriteDTO> getStudyModules() {
        return studyModules;
    }

    public void setStudyModules(List<StudyModuleWriteDTO> studyModules) {
        this.studyModules = studyModules;
    }
}
