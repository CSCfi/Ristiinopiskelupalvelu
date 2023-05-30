package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.StudyElementSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.DeleteFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;

import java.util.List;
import java.util.Optional;

public interface StudyElementService<W extends AbstractStudyElementWriteDTO, T extends StudyElementEntity, R extends AbstractStudyElementReadDTO> extends Service<W, T, R> {

    CompositeIdentifiedEntityModificationResult createAll(List<W> studyElements, String organisationId) throws CreateFailedException;

    T update(JsonNode updateJson, String organisationId) throws UpdateFailedException;

    T deleteByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId) throws DeleteFailedException;

    T delete(String studyElementId, String organizingOrganisationId, boolean deleteSubElements) throws DeleteFailedException;

    Optional<T> findByIdAndOrganizingOrganisationIds(String id, List<String> organizingOrganisationIds) throws FindFailedException;

    List<T> findByIdsAndOrganizingOrganisationIds(List<String> ids, List<String> organizingOrganisationIds) throws FindFailedException;

    List<T> findAllByOrganizingOrganisationIds(List<String> organizingOrganisationIds) throws FindFailedException;

    List<T> findAllByCooperationNetworkIds(List<String> cooperationNetworkIds) throws FindFailedException;

    Optional<T> findByIdAndCooperationNetworkIds(String id, List<String> cooperationNetworkIds) throws FindFailedException;

    List<T> findByIdsAndCooperationNetworkIds(List<String> ids, List<String> cooperationNetworkIds) throws FindFailedException;

    Optional<T> findByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId) throws FindFailedException;

    List<T> findByStudyElementReference(String referenceIdentifier, String referenceOrganizer) throws FindFailedException;

    StudyElementSearchResults<R> search(String organisationId, StudyElementSearchParameters<T> searchParameters) throws FindFailedException;
}
