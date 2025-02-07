package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.service.result.GenericEntityModificationResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NetworkService extends Service<NetworkWriteDTO, NetworkEntity, NetworkReadDTO> {

    List<GenericEntityModificationResult> create(NetworkEntity entity) throws CreateFailedException;

    Optional<NetworkEntity> findNetworkById(String networkId) throws FindFailedException;

    Optional<NetworkEntity> findValidNetworkById(String networkId) throws FindFailedException;

    Optional<NetworkEntity> findValidNetworkById(String networkId, boolean validityStartValid, boolean validityEndValid) throws FindFailedException;

    NetworkEntity update(JsonNode updateJson) throws FindFailedException, UpdateFailedException;

    NetworkEntity markAsDeleted(NetworkEntity networkEntity) throws UpdateFailedException;

    List<NetworkEntity> findAllNetworksByOrganisationIdAndNetworkNameByLanguage(String orgId, String name, String lang, Pageable pageable) throws FindFailedException;

    Optional<NetworkEntity> findNetworkByOrganisationIdAndNetworkId(String orgId, String id) throws FindFailedException;

    List<NetworkEntity> findAllNetworksByOrganisationId(String id, Pageable pageable) throws FindFailedException;

    List<String> findOrganisationIdsFromOrganisationNetworks(String organisationId, Pageable pageable) throws FindFailedException;

    List<String> findOrganisationIdsFromNetworks(List<String> networkIds);

    List<String> findNetworkIdsFromOrganisationNetworks(String organisationId, Pageable pageable) throws FindFailedException;

    List<NetworkEntity> findAllValidNetworksWhereOrganisationIsValid(String organisationId, Pageable pageable) throws FindFailedException;
}
