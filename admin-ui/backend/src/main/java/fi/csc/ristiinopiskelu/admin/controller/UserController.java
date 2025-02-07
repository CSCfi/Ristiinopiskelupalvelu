package fi.csc.ristiinopiskelu.admin.controller;

import fi.csc.ristiinopiskelu.admin.dto.UserInformationDTO;
import fi.csc.ristiinopiskelu.admin.dto.UserOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationDetails;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethUserDetails;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/user")
@RestController
public class UserController {

    @Autowired
    private OrganisationRepository organisationRepository;

    @GetMapping(path = "roles")
    public List<Role> checkLogin() {
        ShibbolethUserDetails userDetails = ShibbolethUserDetails.getCurrent();
        return userDetails.getRoles();
    }

    @GetMapping(path = "")
    public UserInformationDTO getLoginInformation() {
        ShibbolethAuthenticationDetails authenticationDetails = ShibbolethAuthenticationDetails.getCurrent();
        ShibbolethUserDetails userDetails = ShibbolethUserDetails.getCurrent();

        UserInformationDTO userInformation = new UserInformationDTO();

        userInformation.setEmail(authenticationDetails.getEmail());
        userInformation.setFirstnames(authenticationDetails.getFirstNames());
        userInformation.setLastname(authenticationDetails.getSurName());
        userInformation.setGivenname(authenticationDetails.getGivenName());
        userInformation.setFullname(authenticationDetails.getFullName());
        userInformation.setRoles(userDetails.getAuthorities().stream().map(ga -> Role.from(ga.getAuthority())).toList());

        if(StringUtils.hasText(authenticationDetails.getOrganisation())) {
            OrganisationEntity organisationEntity = organisationRepository.findById(authenticationDetails.getOrganisation()).orElse(null);
            if (organisationEntity != null) {
                UserOrganisationDTO userOrganisation = new UserOrganisationDTO();
                userOrganisation.setId(organisationEntity.getId());
                userOrganisation.setName(organisationEntity.getOrganisationName());
                userInformation.setUserOrganisation(userOrganisation);
            }
        }

        return userInformation;
    }
}
