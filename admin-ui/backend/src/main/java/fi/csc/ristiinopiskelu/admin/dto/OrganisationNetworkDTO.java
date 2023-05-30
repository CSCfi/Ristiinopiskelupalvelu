package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;

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
}
