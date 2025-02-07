package fi.uta.ristiinopiskelu.messaging.message.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class CreateStudyModuleRequest extends AbstractRequest {

    private List<StudyModuleWriteDTO> studyModules;

    @ArraySchema(schema = @Schema(name = "studyModules", implementation = StudyModuleWriteDTO.class))
    public List<StudyModuleWriteDTO> getStudyModules() {
        return studyModules;
    }

    public void setStudyModules(List<StudyModuleWriteDTO> studyModules) {
        this.studyModules = studyModules;
    }
}
