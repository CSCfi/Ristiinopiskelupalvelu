package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NetworkRepositoryExtended {

    Optional<NetworkEntity> findNetworkById(String networkId);

    Optional<NetworkEntity> findValidNetworkById(String networkId);

    Optional<NetworkEntity> findValidNetworkById(String networkId, boolean validityStartMustBeValid, boolean validityEndMustBeValid);

    List<NetworkEntity> findAllNetworksByOrganisationIdAndNetworkNameByLanguage(String orgId, String name, String lang, Pageable pageable);

    Optional<NetworkEntity> findNetworkByOrganisationIdAndNetworkId(String orgId, String id);

    List<NetworkEntity> findAllNetworksByOrganisationId(String id, Pageable pageable);
}
