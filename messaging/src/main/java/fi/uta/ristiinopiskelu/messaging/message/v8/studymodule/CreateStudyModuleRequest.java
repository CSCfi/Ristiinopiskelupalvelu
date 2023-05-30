package fi.uta.ristiinopiskelu.messaging.message.v8.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyModuleRequestDTO;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

import java.util.List;

public class CreateStudyModuleRequest extends AbstractRequest {

    private List<CreateStudyModuleRequestDTO> studyModules;

    public List<CreateStudyModuleRequestDTO> getStudyModules() {
        return studyModules;
    }

    public void setStudyModules(List<CreateStudyModuleRequestDTO> studyModules) {
        this.studyModules = studyModules;
    }
}
