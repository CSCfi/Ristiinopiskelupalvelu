package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Document(indexName = "kayttajaroolit", createIndex = false)
public class UserRoleEntity extends GenericEntity implements Serializable {

    private String organisation;
    private String eppn;
    private Role role;

    public String getEppn() {
        return eppn;
    }

    public void setEppn(String eppn) {
        this.eppn = eppn;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getOrganisation() {
        return organisation;
    }
}
