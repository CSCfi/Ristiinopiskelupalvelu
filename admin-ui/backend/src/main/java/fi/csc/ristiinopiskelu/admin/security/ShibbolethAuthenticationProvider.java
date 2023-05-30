package fi.csc.ristiinopiskelu.admin.security;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import fi.uta.ristiinopiskelu.datamodel.entity.UserRoleEntity;
import fi.uta.ristiinopiskelu.persistence.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShibbolethAuthenticationProvider implements AuthenticationProvider {

    private static Logger logger = LoggerFactory.getLogger(ShibbolethAuthenticationProvider.class);

    @Autowired
    private UserRoleRepository userRoleRepository;

    public ShibbolethAuthenticationProvider() {
        super();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ShibbolethAuthenticationToken shibToken = (ShibbolethAuthenticationToken) authentication;

        logger.debug("Yritetään tunnistaa: {}", shibToken);

        String organisation = shibToken.getAttributes().get("organisation");
        AppUserDetails userDetails = new AppUserDetails();
        userDetails.setEppn((String) shibToken.getPrincipal());

        List<GrantedAuthority> authorities = new ArrayList<>();

        List<UserRoleEntity> userRoles = this.userRoleRepository.findByEppn(userDetails.getEppn());

        if(!CollectionUtils.isEmpty(userRoles)) {
            List<UserRoleEntity> adminRoles = userRoles.stream().filter(ur -> ur.getRole() == Role.ADMIN).collect(Collectors.toList());
            List<UserRoleEntity> superUserRoles = userRoles.stream().filter(ur -> ur.getRole() == Role.SUPERUSER).collect(Collectors.toList());

            if(adminRoles.size() > 1 || superUserRoles.size() > 1) {
                throw new AccessDeniedException("Käyttäjällä on duplikaattirooleja");
            }

            UserRoleEntity adminRole = adminRoles.size() == 1 ? adminRoles.get(0) : null;
            UserRoleEntity superUserRole = superUserRoles.size() == 1 ? superUserRoles.get(0) : null;

            if(adminRole != null) {
                if(StringUtils.hasText(adminRole.getOrganisation())) {
                    organisation = adminRole.getOrganisation();
                    authorities.add(new SimpleGrantedAuthority(Role.ADMIN.getPrefixedRole()));
                } else {
                    throw new AccessDeniedException("ADMIN-roolilla organisaatiotieto on pakollinen");
                }
            }

            if(superUserRole != null) {
                authorities.add(new SimpleGrantedAuthority(Role.SUPERUSER.getPrefixedRole()));
            }
        }

        Errors errors = AttributesValidator.validate(shibToken);
        if (errors.hasErrors()) {
            logger.warn("Puutteelliset attribuutit käyttäjälle '{}': {}",
                    userDetails.getEppn(), errors.getAllErrors());
            throw new AccessDeniedException("Puutteelliset Shibboleth-attribuutit.");
        }

        userDetails.setFullname(shibToken.getAttributes().get("cn"));
        userDetails.setGivenName(shibToken.getAttributes().get("givenName"));
        userDetails.setFirstnames(shibToken.getAttributes().get("funetEduPersonGivenNames"));
        userDetails.setSurname(shibToken.getAttributes().get("sn"));
        userDetails.setEmail(shibToken.getAttributes().get("mail"));
        userDetails.setOrganisation(organisation);
        userDetails.setAuthorities(authorities);

        return new ShibbolethAuthenticationToken(userDetails, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ShibbolethAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
