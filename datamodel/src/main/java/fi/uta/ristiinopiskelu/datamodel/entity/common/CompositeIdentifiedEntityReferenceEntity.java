package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.Transient;

@Data
@NoArgsConstructor
public class CompositeIdentifiedEntityReferenceEntity {

    private String referenceIdentifier;
    private String referenceOrganizer;
    private CompositeIdentifiedEntityTypeEntity type;

    public CompositeIdentifiedEntityReferenceEntity(String referenceIdentifier, String referenceOrganizer, CompositeIdentifiedEntityTypeEntity type) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.type = type;
    }

    @Transient
    public static CompositeIdentifiedEntityReferenceEntity from(CompositeIdentifiedEntity entity) {
        return new CompositeIdentifiedEntityReferenceEntity(entity.getElementId(), entity.getOrganizingOrganisationId(), entity.getType());
    }
}
