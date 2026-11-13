package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
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
@Tag(name = "organisations", description = "Organisaatiot v8")
@RequestMapping("/api/v8/organisations")
@RestController
public class OrganisationControllerV8 extends AbstractController {

    @Autowired
    private OrganisationService organisationService;

    @Operation(summary = "Hae kaikki organisaatiot", description = "Operaatio palauttaa kaikki organisaatiot. Operaatio käyttää sivutusta " +
        "ja palauttaa oletuksena 100 ensimmäistä tulosta. ", tags = "organisations")
    @GetMapping
    public List<Organisation> findAllOrganisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                       @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                   @RequestParam(defaultValue = "0", required = false) Integer page,
                                                   @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        return super.mapToDto(organisationService.findAll(PageRequest.of(page, pageSize)),
            Organisation.class, fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO.class);
    }

    @Operation(summary = "Hae organisaatio tunnisteen perusteella", description = "Operaatio hakee yksittäisen organisaation tiedot organisaation " +
        "tunnisteen perusteella.", tags = "organisations")
    @GetMapping(value = "/{organisationTkCode}")
    public List<Organisation> findOrganisationByOrganisationTkCode(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                                       @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                                   @PathVariable String organisationTkCode) {
        // "organisationTkCode" is mapped to document id, see MapperConfig
        return organisationService.findById(organisationTkCode)
            .map(organisation -> super.mapToDto(Collections.singletonList(organisation), Organisation.class,
                fi.uta.ristiinopiskelu.datamodel.dto.current.read.organisation.OrganisationReadDTO.class))
            .orElse(Collections.emptyList());
    }
}
