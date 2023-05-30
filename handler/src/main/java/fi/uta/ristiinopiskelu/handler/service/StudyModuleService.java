package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;

public interface StudyModuleService extends StudyElementService<StudyModuleWriteDTO, StudyModuleEntity, StudyModuleReadDTO> {
    
}
