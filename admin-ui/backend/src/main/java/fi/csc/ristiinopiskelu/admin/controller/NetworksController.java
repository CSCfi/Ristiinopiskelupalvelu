package fi.csc.ristiinopiskelu.admin.controller;

import fi.csc.ristiinopiskelu.admin.security.AppUserDetails;
import fi.csc.ristiinopiskelu.admin.services.NetworkService;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequestMapping("/api/networks")
@PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
@RestController
public class NetworksController {

    private static final Logger logger = LoggerFactory.getLogger(NetworksController.class);

    @Autowired
    private NetworkService networkService;

    @RequestMapping(method = RequestMethod.GET)
    public List<NetworkReadDTO> getNetworks(@AuthenticationPrincipal AppUserDetails user) {
        return networkService.findAll(user);
    }

    @RequestMapping(value = "/{networkId}", method = RequestMethod.GET)
    public Optional<NetworkReadDTO> getNetworkById(@PathVariable String networkId, @AuthenticationPrincipal AppUserDetails user) {
       return networkService.findById(networkId, user);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.DELETE)
    public void deleteNetworkById(@PathVariable String networkId) {
        networkService.deleteById(networkId);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<NetworkReadDTO> findNetworksByName(@RequestParam String name, @RequestParam String lang, @AuthenticationPrincipal AppUserDetails user) {
        return networkService.findNetworksByName(name, lang, user);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void updateNetwork(@RequestBody NetworkWriteDTO network, @AuthenticationPrincipal AppUserDetails user) {
        networkService.update(network, user);
    }

    @RequestMapping(value = "/new", method = RequestMethod.GET)
    public NetworkWriteDTO newNetwork(@AuthenticationPrincipal AppUserDetails user) {
        NetworkWriteDTO network = new NetworkWriteDTO();
        network.setName(new LocalisedString("Uusi","New","Ny"));
        network.setAbbreviation("new");
        network.setPublished(false);
        network.setId(UUID.randomUUID().toString());

        NetworkOrganisation ownOrganisation = new NetworkOrganisation();
        ownOrganisation.setOrganisationTkCode(user.getOrganisation());
        ownOrganisation.setValidityInNetwork(new Validity());
        ownOrganisation.setIsCoordinator(true);

        network.setOrganisations(Collections.singletonList(ownOrganisation));

        return network;
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public void addNetwork(@AuthenticationPrincipal AppUserDetails user, @RequestBody NetworkWriteDTO network) {
        networkService.create(network, user);
    }
}
