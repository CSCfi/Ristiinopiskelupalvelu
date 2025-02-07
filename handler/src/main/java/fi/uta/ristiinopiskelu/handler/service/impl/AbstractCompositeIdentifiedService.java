package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.service.CompositeIdentifiedService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

public abstract class AbstractCompositeIdentifiedService<W, T extends CompositeIdentifiedEntity, R> extends AbstractService<W, T, R> implements CompositeIdentifiedService<W, T, R> {

    @Autowired
    private NetworkService networkService;

    @Autowired
    private OrganisationService organisationService;

    protected abstract ExtendedRepository<T, String> getRepository();

    // Empty constructor for mocking in tests do not use outside of tests
    public AbstractCompositeIdentifiedService() {}

    public AbstractCompositeIdentifiedService(Class<W> writeDtoClazz, Class<T> entityClazz, Class<R> readDtoClass) {
        super(writeDtoClazz, entityClazz, readDtoClass);
    }

    protected T setCooperationNetworkData(T entity) {
        if(CollectionUtils.isEmpty(entity.getCooperationNetworks())) {
            return entity;
        }

        for(CooperationNetwork cooperationNetwork : entity.getCooperationNetworks()) {
            NetworkEntity networkEntity = networkService.findById(cooperationNetwork.getId()).orElse(null);
            // Should this fail if no network is found with given id since there we are saving incorrect data?
            // This should never happen, networks should always be validated before coming here
            // If this would fail here, there is a chance that createAll would create some study elements in message but reply failure message
            if(networkEntity != null) {
                cooperationNetwork.setName(networkEntity.getName());
            }
        }
        return entity;
    }

    protected T setOrganizingOrganisationId(T entity) {
        entity.setOrganizingOrganisationId(this.getOrganizingOrganisationIdFromReferences(entity.getOrganisationReferences()));
        return entity;
    }

    protected String getOrganizingOrganisationIdFromReferences(List<OrganisationReference> organisationReferences) {
        if(!CollectionUtils.isEmpty(organisationReferences)) {
            for(OrganisationReference ref : organisationReferences) {
                if (ref.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER) {
                    return ref.getOrganisation().getOrganisationTkCode();
                }
            }
        }

        throw new IllegalArgumentException("No organisations with organiser role found");
    }

    protected T fillMissingOrganisationInfo(T entity) {
        if(CollectionUtils.isEmpty(entity.getOrganisationReferences())) {
            return entity;
        }

        for(OrganisationReference reference : entity.getOrganisationReferences()) {
            Organisation organisation = reference.getOrganisation();
            organisationService.fillMissingValuesById(organisation, organisation.getOrganisationTkCode());
        }

        return entity;
    }

    protected T setUniqueId(T entity) {
        Assert.hasText(entity.getOrganizingOrganisationId(), "Entity has no organizingOrganisationId?");
        Assert.hasText(entity.getElementId(), "Entity has no elementId?");
        entity.setId(String.format("%s-%s", entity.getOrganizingOrganisationId(), entity.getElementId()));
        return entity;
    }
}
