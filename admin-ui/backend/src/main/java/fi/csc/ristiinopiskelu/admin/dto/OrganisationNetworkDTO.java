package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class OrganisationNetworkDTO {
    
    private String id;
    private LocalisedString name;
    private String abbreviation;
    private Validity validity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public static OrganisationNetworkDTO from(NetworkEntity network) {
        if(network == null) {
            return null;
        }

        OrganisationNetworkDTO dto = new OrganisationNetworkDTO();
        dto.setId(network.getId());
        dto.setName(LocalisedString.from(network.getName()));
        dto.setAbbreviation(network.getAbbreviation());
        dto.setValidity(Validity.from(network.getValidity()));
        return dto;
    }

    public static List<OrganisationNetworkDTO> from(List<NetworkEntity> networks) {
        if(CollectionUtils.isEmpty(networks)) {
            return Collections.emptyList();
        }

        return networks.stream()
            .map(OrganisationNetworkDTO::from)
            .toList();
    }
}
