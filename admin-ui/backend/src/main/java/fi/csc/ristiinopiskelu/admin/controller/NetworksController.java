package fi.csc.ristiinopiskelu.admin.controller;

import fi.csc.ristiinopiskelu.admin.services.NetworkService;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("/api/networks")
@PreAuthorize("isAuthenticated() and hasAnyRole('ADMIN','SUPERUSER')")
@RestController
public class NetworksController {

    private static final Logger logger = LoggerFactory.getLogger(NetworksController.class);

    @Autowired
    private NetworkService networkService;

    @RequestMapping(method = RequestMethod.GET)
    public List<NetworkReadDTO> getNetworks() {
        return networkService.findAll();
    }

    @RequestMapping(value = "/{networkId}", method = RequestMethod.GET)
    public Optional<NetworkReadDTO> getNetworkById(@PathVariable String networkId) {
       return networkService.findById(networkId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{networkId}", method = RequestMethod.DELETE)
    public void deleteNetworkById(@PathVariable String networkId) {
        networkService.deleteById(networkId);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<NetworkReadDTO> findNetworksByName(@RequestParam String name, @RequestParam String lang) {
        return networkService.findNetworksByName(name, lang);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void updateNetwork(@RequestBody NetworkWriteDTO network) {
        networkService.update(network);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void addNetwork(@RequestBody NetworkWriteDTO network) {
        networkService.create(network);
    }
}
