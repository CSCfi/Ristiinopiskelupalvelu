package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;

public class UserOrganisationDTO {
    private String id;
    private LocalisedString name;

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
}
