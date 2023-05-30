package fi.csc.ristiinopiskelu.admin.dto;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;

import java.util.List;

public class UserInformationDTO {
    private String givenname;
    private String firstnames;
    private String lastname;
    private String email;
    private UserOrganisationDTO userOrganisation;
    private String fullname;
    private List<Role> roles;

    public UserOrganisationDTO getUserOrganisation() {
        return userOrganisation;
    }

    public void setUserOrganisation(UserOrganisationDTO userOrganisation) {
        this.userOrganisation = userOrganisation;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getFirstnames() {
        return firstnames;
    }

    public void setFirstnames(String firstnames) {
        this.firstnames = firstnames;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
