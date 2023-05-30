package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType;

import java.util.Objects;

public class RegistrationSelectionIdentifier {
    private String id;
    private RegistrationSelectionItemType type;
    private String organisationId;

    public RegistrationSelectionIdentifier() {

    }

    public RegistrationSelectionIdentifier(String receivingOganisationTkCode, RegistrationSelection selection) {
        this.id = selection.getSelectionItemId();
        this.type = selection.getSelectionItemType();
        this.organisationId = receivingOganisationTkCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RegistrationSelectionItemType getType() {
        return type;
    }

    public void setType(RegistrationSelectionItemType type) {
        this.type = type;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistrationSelectionIdentifier)) return false;
        RegistrationSelectionIdentifier that = (RegistrationSelectionIdentifier) o;
        return Objects.equals(id, that.id) &&
                type == that.type &&
                Objects.equals(organisationId, that.organisationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, organisationId);
    }
}
