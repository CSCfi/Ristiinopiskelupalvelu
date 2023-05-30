package fi.uta.ristiinopiskelu.datamodel.dto.v8.network;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NetworkOrganisation {
    @JsonProperty("organisationTkCode")
    private String organisationTkCode = null;

    @JsonProperty("isCoordinator")
    private Boolean isCoordinator = null;

    @JsonProperty("validityInNetwork")
    private Validity validityInNetwork = null;

    public NetworkOrganisation() {

    }

    public NetworkOrganisation(String organisationTkCode, Boolean isCoordinator, Validity validityInNetwork) {
        this.organisationTkCode = organisationTkCode;
        this.isCoordinator = isCoordinator;
        this.validityInNetwork = validityInNetwork;
    }

    public String getOrganisationTkCode() {
        return organisationTkCode;
    }

    public void setOrganisationTkCode(String organisationTkCode) {
        this.organisationTkCode = organisationTkCode;
    }

    public Boolean getIsCoordinator() {
        return isCoordinator;
    }

    public void setIsCoordinator(Boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }

    public Validity getValidityInNetwork() {
        return validityInNetwork;
    }

    public void setValidityInNetwork(Validity validityInNetwork) {
        this.validityInNetwork = validityInNetwork;
    }
}
