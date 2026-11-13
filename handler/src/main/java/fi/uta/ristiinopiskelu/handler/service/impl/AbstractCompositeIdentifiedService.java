package fi.uta.ristiinopiskelu.handler.service.impl;

import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationDataEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationRoleEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.OrganizingOrganisationMissingValidationException;
import fi.uta.ristiinopiskelu.handler.service.CompositeIdentifiedService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.persistence.repository.ExtendedRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

        for(CooperationNetworkEntity cooperationNetwork : entity.getCooperationNetworks()) {
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

    protected String getOrganizingOrganisationIdFromReferences(List<OrganisationReferenceEntity> organisationReferences) {
        if(!CollectionUtils.isEmpty(organisationReferences)) {
            for(OrganisationReferenceEntity ref : organisationReferences) {
                if (ref.getOrganisationRole() == OrganisationRoleEntity.ROLE_MAIN_ORGANIZER) {
                    return ref.getTarget().getOrganisationTkCode();
                }
            }
        }

        throw new OrganizingOrganisationMissingValidationException("No organisations with organiser role found");
    }

    protected T fillMissingOrganisationInfo(T entity) {
        if(CollectionUtils.isEmpty(entity.getOrganisationReferences())) {
            return entity;
        }

        for(OrganisationReferenceEntity reference : entity.getOrganisationReferences()) {
            OrganisationDataEntity organisation = reference.getTarget();
            organisationService.fillMissingValuesById(organisation, organisation.getOrganisationTkCode());
        }

        return entity;
    }
}
