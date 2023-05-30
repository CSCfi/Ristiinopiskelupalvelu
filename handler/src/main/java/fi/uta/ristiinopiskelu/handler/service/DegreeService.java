package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree.DegreeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;

public interface DegreeService extends StudyElementService<DegreeWriteDTO, DegreeEntity, DegreeReadDTO> {

}
