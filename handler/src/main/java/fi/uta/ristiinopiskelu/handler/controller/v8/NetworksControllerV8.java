package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
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

@Tag(name = "networks", description = "Verkostot v8")
@RequestMapping("/api/v8/networks")
@RestController
public class NetworksControllerV8 extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(NetworksControllerV8.class);

    @Autowired
    private NetworkService networkService;

    @Operation(summary = "Hae kaikki verkostot", description = "Operaatio palauttaa kaikki verkostot." +
                " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta. ", tags = "networks")
    @RequestMapping(method = RequestMethod.GET)
    public List<Network> findAllNetworks(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                             @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                         @RequestParam(defaultValue = "0", required = false) Integer page,
                                         @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return super.mapToDto(networkService.findAllNetworksByOrganisationId(SSL_CLIENT_S_DN_O, PageRequest.of(page, pageSize)),
            Network.class, fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO.class);
    }

    @Operation(summary = "Hae verkosto organisation tunnisteen perusteella", description = "Operaatio hakee yksittäisen verkoston tiedot organisaation tunnisteen perusteella.",
        tags="networks")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public List<Network> findByNetworkById(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                               @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @PathVariable String id) {
        Optional<NetworkEntity> network = networkService.findNetworkByOrganisationIdAndNetworkId(SSL_CLIENT_S_DN_O, id);
        return network.isPresent() ?
            super.mapToDto(Collections.singletonList(network.get()), Network.class, fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO.class) :
            Collections.emptyList();
    }

    @Operation(summary = "Hae verkostoa nimen perusteella", description = "Operaatio hakee verkostojen tietoja annetun nimen perusteella. Myös nimen kieli on annettava" +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags="networks")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<Network> findNetworkByName(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                 @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                           @Parameter(description = "Organisaation nimi") @RequestParam String name,
                                           @Parameter(description = "Nimen kieli") @RequestParam String lang,
                                           @RequestParam(defaultValue = "0", required = false) Integer page,
                                           @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return super.mapToDto(networkService.findAllNetworksByOrganisationIdAndNetworkNameByLanguage(SSL_CLIENT_S_DN_O,name, lang, PageRequest.of(page, pageSize)),
            Network.class, fi.uta.ristiinopiskelu.datamodel.dto.current.read.network.NetworkReadDTO.class);
    }
}
