package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;

public class CompositeIdentifiedEntityReference {

    private String referenceIdentifier;
    private String referenceOrganizer;
    private CompositeIdentifiedEntityType type;

    public CompositeIdentifiedEntityReference() {
        
    }

    public CompositeIdentifiedEntityReference(String referenceIdentifier, String referenceOrganizer, CompositeIdentifiedEntityType type) {
        this.referenceIdentifier = referenceIdentifier;
        this.referenceOrganizer = referenceOrganizer;
        this.type = type;
    }

    public String getReferenceIdentifier() {
        return referenceIdentifier;
    }

    public void setReferenceIdentifier(String referenceIdentifier) {
        this.referenceIdentifier = referenceIdentifier;
    }

    public String getReferenceOrganizer() {
        return referenceOrganizer;
    }

    public void setReferenceOrganizer(String referenceOrganizer) {
        this.referenceOrganizer = referenceOrganizer;
    }

    public CompositeIdentifiedEntityType getType() {
        return type;
    }

    public void setType(CompositeIdentifiedEntityType type) {
        this.type = type;
    }

    @JsonIgnore
    public static CompositeIdentifiedEntityReference from(CompositeIdentifiedEntity entity) {
        return new CompositeIdentifiedEntityReference(entity.getElementId(), entity.getOrganizingOrganisationId(), CompositeIdentifiedEntityType.valueOf(entity.getType().name()));
    }

    @JsonIgnore
    public static CompositeIdentifiedEntityReference from(StudyElement studyElement, String organizingOrganisationId) {
        return new CompositeIdentifiedEntityReference(studyElement.getStudyElementId(), organizingOrganisationId, CompositeIdentifiedEntityType.from(studyElement.getType()));
    }
}
