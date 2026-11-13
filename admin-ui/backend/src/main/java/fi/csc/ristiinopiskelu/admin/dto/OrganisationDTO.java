package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;

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

    public static OrganisationDTO from(OrganisationEntity org, List<NetworkEntity> networks) {
        if(org == null) {
            return null;
        }

        OrganisationDTO dto = new OrganisationDTO();
        dto.setId(org.getId());
        dto.setOrganisationIdentifier(org.getOrganisationIdentifier());
        dto.setOrganisationName(LocalisedString.from(org.getOrganisationName()));
        dto.setUnitName(LocalisedString.from(org.getUnitName()));
        dto.setNameDescription(LocalisedString.from(org.getNameDescription()));
        dto.setMunicipalityCode(org.getMunicipalityCode());
        dto.setUrl(org.getUrl());
        dto.setPostalAddress(Address.from(org.getPostalAddress()));
        dto.setStreetAddress(Address.from(org.getStreetAddress()));
        dto.setPhone(Phone.from(org.getPhone()));
        dto.setQueue(org.getQueue());
        dto.setAdministratorEmail(org.getAdministratorEmail());
        dto.setNotificationsEnabled(org.isNotificationsEnabled());
        dto.setSchemaVersion(org.getSchemaVersion());
        dto.setNetworks(OrganisationNetworkDTO.from(networks));
        return dto;
    }
}
