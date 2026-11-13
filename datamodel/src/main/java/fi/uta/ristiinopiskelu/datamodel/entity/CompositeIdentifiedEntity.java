package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OrganisationReferenceEntity;

import java.time.OffsetDateTime;
import java.util.List;

public abstract class CompositeIdentifiedEntity extends GenericEntity {

    public abstract String getElementId();

    public abstract String getOrganizingOrganisationId();

    public abstract void setOrganizingOrganisationId(String id);

    public abstract CompositeIdentifiedEntityTypeEntity getType();

    public abstract List<CooperationNetworkEntity> getCooperationNetworks();

    public abstract List<OrganisationReferenceEntity> getOrganisationReferences();

    public abstract OffsetDateTime getUpdateTime();
}
