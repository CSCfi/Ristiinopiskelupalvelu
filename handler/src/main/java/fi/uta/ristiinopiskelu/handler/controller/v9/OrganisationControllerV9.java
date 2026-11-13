package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@Tag(name = "organisations", description = "Organisaatiot v9")
@RequestMapping("/api/v9/organisations")
@RestController
public class OrganisationControllerV9 {

    @Autowired
    private OrganisationService organisationService;

    @Operation(summary = "Hae kaikki organisaatiot", description = "Operaatio palauttaa kaikki organisaatiot. Operaatio käyttää sivutusta " +
        "ja palauttaa oletuksena 100 ensimmäistä tulosta. ", tags = "organisations")
    @GetMapping
    public List<OrganisationReadDTO> findAllOrganisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                          @RequestParam(defaultValue = "0", required = false) Integer page,
                                                          @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return organisationService.findAll(PageRequest.of(page, pageSize)).stream()
            .map(organisationService::toReadDTO)
            .toList();
    }

    @Operation(summary = "Hae organisaatio tunnisteen perusteella", description = "Operaatio hakee yksittäisen organisaation tiedot organisaation " +
        "tunnisteen perusteella.", tags = "organisations")
    @GetMapping(value = "/{organisationTkCode}")
    public List<OrganisationReadDTO> findOrganisationByOrganisationTkCode(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                                          @PathVariable String organisationTkCode) {
        // "organisationTkCode" is mapped to document id, see MapperConfig
        return organisationService.findById(organisationTkCode)
            .map(organisation -> Collections.singletonList(organisationService.toReadDTO(organisation)))
            .orElse(Collections.emptyList());
    }
}
