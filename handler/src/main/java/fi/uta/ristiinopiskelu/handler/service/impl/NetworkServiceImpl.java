package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class NetworkServiceImpl extends AbstractService<NetworkWriteDTO, NetworkEntity, NetworkReadDTO> implements NetworkService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceImpl.class);

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    public NetworkServiceImpl() {
        super(NetworkWriteDTO.class, NetworkEntity.class, NetworkReadDTO.class);
    }

    @Override
    protected ExtendedRepository<NetworkEntity, String> getRepository() {
        return networkRepository;
    }

    @Override
    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    // network is allowed to be created with a custom id
    @Override
    protected boolean isValidateId() {
        return false;
    }

    @Override
    public Optional<NetworkEntity> findValidNetworkById(String networkId) throws FindFailedException {
        Assert.hasLength(networkId, "Id cannot be empty");
        try {
            return networkRepository.findValidNetworkById(networkId);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), networkId, e);
        }
    }

    @Override
    public NetworkEntity update(JsonNode updateJson) throws UpdateFailedException {
        Assert.notNull(updateJson, "Network jsonNode cannot be null");
        Assert.notNull(updateJson.get("id"), "Network json must have id field");

        String networkId = updateJson.get("id").asText();

        try {
            NetworkEntity original = networkRepository.findById(networkId)
                    .orElseThrow(() -> new EntityNotFoundException(NetworkEntity.class, networkId));
            ObjectReader reader = objectMapper.readerForUpdating(original);
            NetworkEntity updatedNetWorkEntity = reader.readValue(updateJson);
            if (original.isPublished() && !updatedNetWorkEntity.isPublished()) {
                throw new UpdateFailedException("Published network can not be marked as unpublished");
            }

            return networkRepository.update(updatedNetWorkEntity);
        } catch(Exception e) {
            throw new UpdateFailedException(getEntityClass(), networkId, e);
        }
    }

    @Override
    public NetworkEntity markAsDeleted(NetworkEntity networkEntity) throws UpdateFailedException {
        Assert.notNull(networkEntity, "NetworkEntity cannot be null");
        networkEntity.setDeleted(true);
        networkEntity.setDeletedTimestamp(OffsetDateTime.now());

        try {
            return networkRepository.update(networkEntity);
        } catch(Exception e) {
            throw new UpdateFailedException(getEntityClass(), networkEntity.getId(), e);
        }

    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationIdAndNetworkNameByLanguage(String orgId, String name, String lang, Pageable pageable) throws FindFailedException {
        try {
            return networkRepository.findAllNetworksByOrganisationIdAndNetworkNameByLanguage(orgId, name, lang, pageable);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public Optional<NetworkEntity> findNetworkByOrganisationIdAndNetworkId(String orgId, String id) throws FindFailedException {
        try {
            return networkRepository.findNetworkByOrganisationIdAndNetworkId(orgId, id);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), e);
        }
    }

    @Override
    public List<NetworkEntity> findAllNetworksByOrganisationId(String id, Pageable pageable) throws FindFailedException {
        try {
            return networkRepository.findAllNetworksByOrganisationId(id, pageable);
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), id, e);
        }
    }

    @Override
    public List<String> findOrganisationIdsFromOrganisationNetworks(String organisationId, Pageable pageable) throws FindFailedException {
        try {
            List<NetworkEntity> networks = this.networkRepository.findAllNetworksByOrganisationId(organisationId, pageable);
            return networks.stream()
                    .flatMap(network -> network.getOrganisations().stream().map(org ->
                            org.getOrganisationTkCode())).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), organisationId, e);
        }
    }

    @Override
    public List<String> findOrganisationIdsFromNetworks(List<String> networkIds) {
        try {
            List<NetworkEntity> networks = StreamSupport.stream(this.networkRepository.findAllById(networkIds).spliterator(), false).collect(Collectors.toList());
            return networks.stream().flatMap(network -> network.getOrganisations().stream().map(org ->
                    org.getOrganisationTkCode())).collect(Collectors.toList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), networkIds, e);
        }
    }

    @Override
    public List<String> findNetworkIdsFromOrganisationNetworks(String organisationId, Pageable pageable) throws FindFailedException {
        try {
            List<NetworkEntity> networks = this.networkRepository.findAllNetworksByOrganisationId(organisationId, pageable);
            return networks.stream().map(network -> network.getId()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new FindFailedException(getEntityClass(), organisationId, e);
        }
    }

    @Override
    public List<NetworkEntity> findAllValidNetworksWhereOrganisationIsValid(String organisationId, Pageable pageable) {
        List<NetworkEntity> networkEntities = this.findAllNetworksByOrganisationId(organisationId, pageable);
        networkEntities = filterNonValidNetworks(networkEntities, organisationId);
        return networkEntities;
    }

    private List<NetworkEntity> filterNonValidNetworks(List<NetworkEntity> networkEntities, String organisationId) {
        OffsetDateTime now = OffsetDateTime.now();

        // Filter out non valid networks and networks in which requesting organisation is no longer a member
        return networkEntities.stream()
                .filter(network -> network.isPublished())
                .filter(network ->
                        (network.getValidity().getStart() != null && DateUtils.isBeforeOrEqual(network.getValidity().getStart(), now))
                                && (network.getValidity().getEnd() == null || DateUtils.isAfterOrEqual(network.getValidity().getEnd(), now)))
                .filter(network -> network.getOrganisations().stream()
                        .filter(org -> org.getOrganisationTkCode().equals(organisationId))
                        .anyMatch(org ->
                                (org.getValidityInNetwork() != null)
                                        && (org.getValidityInNetwork().getStart() != null && DateUtils.isBeforeOrEqual(org.getValidityInNetwork().getStart(), now))
                                        && (org.getValidityInNetwork().getEnd() == null || DateUtils.isAfterOrEqual(org.getValidityInNetwork().getEnd(), now))
                        )
                )
                .collect(Collectors.toList());
    }
}
