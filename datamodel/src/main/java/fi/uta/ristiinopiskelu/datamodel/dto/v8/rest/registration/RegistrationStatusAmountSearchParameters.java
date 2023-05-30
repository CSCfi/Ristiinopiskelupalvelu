package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.Registration;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.SearchParameters;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class RegistrationStatusAmountSearchParameters implements SearchParameters<Registration> {

    @Schema(description = "Rajaa rekisteröintipyyntöjä verkostojen mukaan.")
    private List<String> networkIdentifiers;

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }
}
