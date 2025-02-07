package fi.csc.ristiinopiskelu.admin.security;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import fi.uta.ristiinopiskelu.datamodel.entity.UserRoleEntity;
import fi.uta.ristiinopiskelu.persistence.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ShibbolethAuthenticationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        String eppn = (String) token.getPrincipal();
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<UserRoleEntity> userRoles = this.userRoleRepository.findByEppn(eppn);

        if(!CollectionUtils.isEmpty(userRoles)) {
            List<UserRoleEntity> adminRoles = userRoles.stream()
                    .filter(ur -> ur.getRole() == Role.ADMIN)
                    .toList();
            List<UserRoleEntity> superUserRoles = userRoles.stream()
                    .filter(ur -> ur.getRole() == Role.SUPERUSER)
                    .toList();

            if(adminRoles.size() > 1 || superUserRoles.size() > 1) {
                throw new UsernameNotFoundException("Käyttäjällä on duplikaattirooleja");
            }

            UserRoleEntity adminRole = adminRoles.stream()
                    .findFirst()
                    .orElse(null);
            UserRoleEntity superUserRole = superUserRoles.stream()
                    .findFirst()
                    .orElse(null);

            if(adminRole != null) {
                if(StringUtils.hasText(adminRole.getOrganisation())) {
                    authorities.add(new SimpleGrantedAuthority(Role.ADMIN.getPrefixedRole()));
                } else {
                    throw new UsernameNotFoundException("ADMIN-roolilla organisaatiotieto on pakollinen");
                }
            }

            if(superUserRole != null) {
                authorities.add(new SimpleGrantedAuthority(Role.SUPERUSER.getPrefixedRole()));
            }
        }

        return new ShibbolethUserDetails(eppn, authorities);
    }
}
