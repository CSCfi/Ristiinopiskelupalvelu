package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RealisationService extends Service<RealisationWriteDTO, RealisationEntity, RealisationReadDTO> {

    RealisationEntity update(JsonNode updateJson, String organisationId) throws UpdateFailedException;

    RealisationEntity delete(String realisationId, String organizingOrganisationId) throws DeleteFailedException;

    Optional<RealisationEntity> findByIdAndOrganizingOrganisationId(String id, String organisationId) throws FindFailedException;

    void deleteByRealisationIdAndOrganizingOrganisationId(String id, String organizingOrganisationId) throws DeleteFailedException;

    List<RealisationEntity> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer) throws FindFailedException;

    RealisationSearchResults search(String organisationId, RealisationSearchParameters searchParameters) throws FindFailedException;

    RealisationSearchResults searchByIds(String organisationId, String realisationId, String organizingOrganisationId,
                                         List<StudyStatus> statuses, Pageable pageable);

    List<RealisationEntity> findByAssessmentItemReference(String referenceIdentifier, String referenceOrganizer, String assessmentItemId);
}
