package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.SearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class RegistrationStatusAmountSearchParameters implements SearchParameters<RegistrationEntity> {

    @Schema(description = "Rajaa rekisteröintipyyntöjä verkostojen mukaan.")
    private List<String> networkIdentifiers;

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
    }
}
