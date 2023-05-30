package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "networks", description = "Verkostot v9")
@RequestMapping("/api/v9/networks")
@RestController
public class NetworksControllerV9 {

    private static final Logger logger = LoggerFactory.getLogger(NetworksControllerV9.class);

    @Autowired
    private NetworkService networkService;

    @Operation(summary = "Hae kaikki verkostot", description = "Operaatio palauttaa kaikki verkostot." +
                " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta. ", tags = "networks")
    @RequestMapping(method = RequestMethod.GET)
    public List<NetworkReadDTO> findAllNetworks(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                             @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                @RequestParam(defaultValue = "0", required = false) Integer page,
                                                @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return networkService.findAllNetworksByOrganisationId(SSL_CLIENT_S_DN_O, PageRequest.of(page, pageSize)).stream().map(networkService::toReadDTO).collect(Collectors.toList());
    }

    @Operation(summary = "Hae verkosto organisation tunnisteen perusteella", description = "Operaatio hakee yksittäisen " +
        "verkoston tiedot organisaation tunnisteen perusteella.", tags="networks")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public List<NetworkReadDTO> findByNetworkById(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                               @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @PathVariable String id) {
        Optional<NetworkEntity> network = networkService.findNetworkByOrganisationIdAndNetworkId(SSL_CLIENT_S_DN_O, id);
        return network.isPresent() ? Collections.singletonList(networkService.toReadDTO(network.get())) : Collections.emptyList();
    }

    @Operation(summary = "Hae verkostoa nimen perusteella", description = "Operaatio hakee verkostojen tietoja annetun nimen perusteella. Myös nimen kieli on annettava" +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags="networks")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<NetworkReadDTO> findNetworkByName(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                 @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                           @Parameter(description = "Organisaation nimi") @RequestParam String name,
                                           @Parameter(description = "Nimen kieli") @RequestParam String lang,
                                           @RequestParam(defaultValue = "0", required = false) Integer page,
                                           @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return networkService.findAllNetworksByOrganisationIdAndNetworkNameByLanguage(SSL_CLIENT_S_DN_O,name, lang, PageRequest.of(page, pageSize)).stream().map(networkService::toReadDTO).collect(Collectors.toList());
    }
}
