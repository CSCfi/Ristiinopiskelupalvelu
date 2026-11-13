package fi.csc.ristiinopiskelu.admin.services;

import fi.csc.ristiinopiskelu.admin.dto.CreateOrUpdateOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.dto.OrganisationDTO;
import fi.csc.ristiinopiskelu.admin.exception.EntityNotFoundException;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationDetails;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethUserDetails;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.AddressEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.PhoneEntity;
import fi.uta.ristiinopiskelu.persistence.repository.MessageSchemaRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public OrganisationDTO create(CreateOrUpdateOrganisationDTO organisation) {
        OrganisationEntity created = organisationRepository.create(toOrganisationEntity(organisation));
        return toOrganisationDto(created);
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

        updateOrganisationEntity(organisation, originalOrganisationEntity);
        originalOrganisationEntity = organisationRepository.update(originalOrganisationEntity);
        return toOrganisationDto(originalOrganisationEntity);
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
                .toList());
        } else {
            organisationRepository.findById(authenticationDetails.getOrganisation()).ifPresent(organisationEntities::add);
        }

        return organisationEntities.stream().map(this::toOrganisationDto).collect(Collectors.toList());
    }

    public List<Integer> getSchemaVersions() {
        return StreamSupport.stream(messageSchemaRepository.findAll().spliterator(), false)
            .map(MessageSchemaEntity::getSchemaVersion).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    private OrganisationEntity toOrganisationEntity(CreateOrUpdateOrganisationDTO source) {
        OrganisationEntity entity = new OrganisationEntity();
        updateOrganisationEntity(source, entity);
        return entity;
    }

    private void updateOrganisationEntity(CreateOrUpdateOrganisationDTO source, OrganisationEntity target) {
        target.setId(source.getId());
        target.setOrganisationIdentifier(source.getOrganisationIdentifier());
        target.setOrganisationName(LocalisedStringEntity.from(source.getOrganisationName()));
        target.setUnitName(LocalisedStringEntity.from(source.getUnitName()));
        target.setNameDescription(LocalisedStringEntity.from(source.getNameDescription()));
        target.setMunicipalityCode(source.getMunicipalityCode());
        target.setUrl(source.getUrl());
        target.setPostalAddress(AddressEntity.from(source.getPostalAddress()));
        target.setStreetAddress(AddressEntity.from(source.getStreetAddress()));
        target.setPhone(PhoneEntity.from(source.getPhone()));
        target.setQueue(source.getQueue());
        target.setAdministratorEmail(source.getAdministratorEmail());
        target.setNotificationsEnabled(source.isNotificationsEnabled());
        target.setSchemaVersion(source.getSchemaVersion());
    }

    private OrganisationDTO toOrganisationDto(OrganisationEntity org) {
        return OrganisationDTO.from(org, networkRepository.findAllNetworksByOrganisationId(org.getId(), Pageable.unpaged()));
    }
}
