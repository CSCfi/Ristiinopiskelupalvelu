package fi.csc.ristiinopiskelu.admin.security;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AppUserDetails implements UserDetails, AppUser {
   
    private String eppn;
    private String organisation;
    private String givenName;
    private String firstnames;
    private String surname;
    private String email;
    private String fullname;
    private Collection<? extends GrantedAuthority> authorities;

    public void setEppn(String eppn) {
        this.eppn = eppn;
    }

    @Override
    public String getEppn() {
        return eppn;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    @Override
    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    @Override
    public String getFirstnames() {
        return firstnames;
    }

    public void setFirstnames(String firstnames) {
        this.firstnames = firstnames;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    @Override
    public boolean hasAuthority(GrantedAuthority authority) {
        return getAuthorities().contains(authority);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return eppn;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isAdmin() {
        return this.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(Role.ADMIN.getPrefixedRole()));
    }

    public boolean isSuperUser() {
        return this.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(Role.SUPERUSER.getPrefixedRole()));
    }

    public List<Role> getRoles() {
        return this.getAuthorities().stream().map(authority -> Role.from(authority.getAuthority())).collect(Collectors.toList());
    }

}
