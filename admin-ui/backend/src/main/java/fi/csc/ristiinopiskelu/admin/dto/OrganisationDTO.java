package fi.csc.ristiinopiskelu.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class OrganisationDTO extends CreateOrUpdateOrganisationDTO {
    
    private List<OrganisationNetworkDTO> networks = new ArrayList<>();

    public List<OrganisationNetworkDTO> getNetworks() {
        return networks;
    }

    public void setNetworks(List<OrganisationNetworkDTO> networks) {
        this.networks = networks;
    }
}
