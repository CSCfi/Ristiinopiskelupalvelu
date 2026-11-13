package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.beans.Transient;

@Data
@EqualsAndHashCode(callSuper = true)
public class RealisationReferenceEntity extends CompositeIdentifiedEntityReferenceEntity {

    public RealisationReferenceEntity() {
        super(null, null, CompositeIdentifiedEntityTypeEntity.REALISATION);
    }

    public RealisationReferenceEntity(String referenceIdentifier, String referenceOrganizer) {
        super(referenceIdentifier, referenceOrganizer, CompositeIdentifiedEntityTypeEntity.REALISATION);
    }

    @Transient
    public static RealisationReferenceEntity from(RealisationEntity entity) {
        return new RealisationReferenceEntity(entity.getRealisationId(), entity.getOrganizingOrganisationId());
    }
}
