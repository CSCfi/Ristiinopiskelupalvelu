package fi.csc.ristiinopiskelu.admin.security.profiles.dev;

import fi.csc.ristiinopiskelu.admin.security.AppUserDetails;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationProvider;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationToken;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

public class DevelopmentShibbolethAuthenticationProvider extends ShibbolethAuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ShibbolethAuthenticationToken original = (ShibbolethAuthenticationToken) super.authenticate(authentication);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(Role.SUPERUSER.getPrefixedRole()));

        AppUserDetails appUserDetails = (AppUserDetails) original.getPrincipal();
        appUserDetails.setAuthorities(authorities);

        return new ShibbolethAuthenticationToken(appUserDetails, authorities);
    }
}
