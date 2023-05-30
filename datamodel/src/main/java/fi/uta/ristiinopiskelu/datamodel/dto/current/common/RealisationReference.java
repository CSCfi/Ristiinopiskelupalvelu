package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;

public class RealisationReference extends CompositeIdentifiedEntityReference {

    public RealisationReference() {
        super(null, null, CompositeIdentifiedEntityType.REALISATION);
    }

    public RealisationReference(String referenceIdentifier, String referenceOrganizer) {
        super(referenceIdentifier, referenceOrganizer, CompositeIdentifiedEntityType.REALISATION);
    }

    @JsonIgnore
    public static RealisationReference from(RealisationEntity entity) {
        return new RealisationReference(entity.getRealisationId(), entity.getOrganizingOrganisationId());
    }
}
