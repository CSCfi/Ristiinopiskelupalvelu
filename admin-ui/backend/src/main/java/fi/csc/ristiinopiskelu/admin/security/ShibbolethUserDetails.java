package fi.csc.ristiinopiskelu.admin.security;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

import java.util.Collection;
import java.util.List;

public class ShibbolethUserDetails implements UserDetails {

    private final String eppn;
    private final Collection<? extends GrantedAuthority> authorities;

    public ShibbolethUserDetails(String eppn, Collection<? extends GrantedAuthority> authorities) {
        this.eppn = eppn;
        this.authorities = authorities;
    }

    public String getEppn() {
        return eppn;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "N/A";
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
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(Role.ADMIN.getPrefixedRole()));
    }

    public boolean isSuperUser() {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(Role.SUPERUSER.getPrefixedRole()));
    }

    public List<Role> getRoles() {
        return authorities.stream()
                .map(authority -> Role.from(authority.getAuthority()))
                .toList();
    }

    public static ShibbolethUserDetails getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof ShibbolethUserDetails shibbolethUserDetails) {
            return shibbolethUserDetails;
        }

        throw new PreAuthenticatedCredentialsNotFoundException("User is not authenticated");
    }
}
