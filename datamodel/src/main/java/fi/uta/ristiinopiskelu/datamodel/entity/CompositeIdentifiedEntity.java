package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;

import java.util.List;

public abstract class CompositeIdentifiedEntity extends GenericEntity {

    public abstract String getElementId();

    public abstract String getOrganizingOrganisationId();

    public abstract void setOrganizingOrganisationId(String id);

    public abstract CompositeIdentifiedEntityType getType();

    public abstract List<CooperationNetwork> getCooperationNetworks();

    public abstract List<OrganisationReference> getOrganisationReferences();
}
