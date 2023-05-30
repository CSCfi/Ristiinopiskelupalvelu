package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.code.CodeReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.code.CodeSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.code.CodeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;

public interface CodeService extends Service<CodeWriteDTO, CodeEntity, CodeReadDTO> {

    CodeSearchResults search(String organisationId, CodeSearchParameters searchParameters) throws FindFailedException;
}
