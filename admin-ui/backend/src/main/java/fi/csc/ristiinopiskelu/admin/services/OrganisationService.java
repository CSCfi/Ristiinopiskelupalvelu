package fi.csc.ristiinopiskelu.admin.services;

import fi.csc.ristiinopiskelu.admin.dto.CreateOrUpdateOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.dto.OrganisationDTO;
import fi.csc.ristiinopiskelu.admin.dto.OrganisationNetworkDTO;
import fi.csc.ristiinopiskelu.admin.exception.EntityNotFoundException;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationDetails;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethUserDetails;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.MessageSchemaRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OrganisationService {

    private static Logger logger = LoggerFactory.getLogger(OrganisationService.class);

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private MessageSchemaRepository messageSchemaRepository;

    @Autowired
    private ModelMapper modelMapper;
    
    public OrganisationDTO create(CreateOrUpdateOrganisationDTO organisation) {
        OrganisationEntity created = organisationRepository.create(modelMapper.map(organisation, OrganisationEntity.class));
        return new OrganisationDtoMapper().apply(created);
    }

    public OrganisationDTO update(String organisationId, CreateOrUpdateOrganisationDTO organisation) {
        ShibbolethUserDetails userDetails = ShibbolethUserDetails.getCurrent();
        ShibbolethAuthenticationDetails authenticationDetails = ShibbolethAuthenticationDetails.getCurrent();

        OrganisationEntity originalOrganisationEntity = organisationRepository.findById(organisationId)
            .orElseThrow(() -> new EntityNotFoundException("Organisation not found"));

        if(!userDetails.isSuperUser()) {
            if(!organisationId.equals(authenticationDetails.getOrganisation())) {
                logger.error("User without super-user role tried to update organisation that is not his organisation.");
                throw new InsufficientAuthenticationException("Organisation id is not the same as user organisation id");
            }

            // Prevent normal admin user from updating system settings
            organisation.setNotificationsEnabled(originalOrganisationEntity.isNotificationsEnabled());
            organisation.setQueue(originalOrganisationEntity.getQueue());
            organisation.setSchemaVersion(originalOrganisationEntity.getSchemaVersion());
        }

        modelMapper.map(organisation, originalOrganisationEntity);
        originalOrganisationEntity = organisationRepository.update(originalOrganisationEntity);
        return new OrganisationDtoMapper().apply(originalOrganisationEntity);
    }

    public void deleteById(String organisationId) {
        organisationRepository.deleteById(organisationId);
    }

    public List<OrganisationDTO> findAll() {
        ShibbolethUserDetails userDetails = ShibbolethUserDetails.getCurrent();
        ShibbolethAuthenticationDetails authenticationDetails = ShibbolethAuthenticationDetails.getCurrent();

        List<OrganisationEntity> organisationEntities = new ArrayList<>();
        if(userDetails.isSuperUser()) {
            organisationEntities.addAll(StreamSupport.stream(organisationRepository.findAll().spliterator(), false)
                .collect(Collectors.toList()));
        } else {
            organisationRepository.findById(authenticationDetails.getOrganisation()).ifPresent(organisationEntities::add);
        }

        return organisationEntities.stream().map(new OrganisationDtoMapper()).collect(Collectors.toList());
    }

    public List<Integer> getSchemaVersions() {
        return StreamSupport.stream(messageSchemaRepository.findAll().spliterator(), false)
            .map(MessageSchemaEntity::getSchemaVersion).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    private class OrganisationDtoMapper implements Function<OrganisationEntity, OrganisationDTO> {

        @Override
        public OrganisationDTO apply(OrganisationEntity org) {
            OrganisationDTO mapped = modelMapper.map(org, OrganisationDTO.class);
            mapped.setNetworks(networkRepository.findAllNetworksByOrganisationId(org.getId(), Pageable.unpaged())
                .stream().map(network -> modelMapper.map(network, OrganisationNetworkDTO.class)).collect(Collectors.toList()));
            return mapped;
        }
    }
}
