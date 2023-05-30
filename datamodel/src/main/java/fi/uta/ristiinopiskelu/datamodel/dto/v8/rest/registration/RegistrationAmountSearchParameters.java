package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.Registration;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemType;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.SearchParameters;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class RegistrationAmountSearchParameters implements SearchParameters<Registration> {

    @Schema(description = "Rajaa rekisteröintipyyntöjä verkostojen mukaan.")
    private List<String> networkIdentifiers;
    @Schema(description = "Rajaa rekisteröintipyyntöjä kohdeopintoelementin tyyppi.")
    private RegistrationSelectionItemType type;

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }

    public void setType(RegistrationSelectionItemType type) {
        this.type = type;
    }

    public RegistrationSelectionItemType getType() {
        return type;
    }
}
