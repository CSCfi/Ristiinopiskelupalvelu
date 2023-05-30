package fi.csc.ristiinopiskelu.admin.controller;

import fi.csc.ristiinopiskelu.admin.dto.UserInformationDTO;
import fi.csc.ristiinopiskelu.admin.dto.UserOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.security.AppUserDetails;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Role;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = auth != null && auth.isAuthenticated();
        boolean hasHakaUserDetails = isAuthenticated && (auth.getPrincipal() != null && (auth.getPrincipal() instanceof AppUserDetails));

        if (!hasHakaUserDetails) {
            throw new AuthenticationCredentialsNotFoundException("Not logged in");
        }

        return ((AppUserDetails) auth.getPrincipal()).getRoles();
    }

    @GetMapping(path = "")
    public UserInformationDTO getLoginInformation(@AuthenticationPrincipal AppUserDetails user) {
        UserInformationDTO userInformation = new UserInformationDTO();
        userInformation.setEmail(user.getEmail());
        userInformation.setFirstnames(user.getFirstnames());
        userInformation.setLastname(user.getSurname());
        userInformation.setGivenname(user.getGivenName());
        userInformation.setFullname(user.getFullname());
        userInformation.setRoles(user.getRoles());

        if(StringUtils.hasText(user.getOrganisation())) {
            OrganisationEntity organisationEntity = organisationRepository.findById(user.getOrganisation()).orElse(null);
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
