package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.organisation.OrganisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;

import java.util.List;

public interface OrganisationService extends Service<OrganisationWriteDTO, OrganisationEntity, OrganisationReadDTO> {

    List<OrganisationEntity> findAllById(List<String> ids);
}
