package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration.RegistrationAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration.RegistrationAmountSearchResult;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration.RegistrationStatusAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration.RegistrationStatusAmountSearchResults;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "registrations", description = "Rekisteröintipyynnöt v8")
@RequestMapping("/api/v8/registrations")
@RestController
public class RegistrationControllerV8 extends AbstractController {

    @Autowired
    private RegistrationService registrationService;

    @Operation(summary = "Hae rekisteröintipyyntöjen tilojen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "tilojen lukumääriä annetuista verkostoista.", tags = "registrations")
    @PostMapping(value = "/search/status/amounts")
    public RegistrationStatusAmountSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                            @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                        @RequestBody RegistrationStatusAmountSearchParameters searchParams) {
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchResults result =
            registrationService.searchStatusAmounts(SSL_CLIENT_S_DN_O,
                super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchParameters.class));
        return super.mapToDto(result, RegistrationStatusAmountSearchResults.class);
    }

    @Operation(summary = "Hae rekisteröintipyyntöjen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen lukumääriä " +
        "annetuilla hakuehdoilla.", tags = "registrations")
    @PostMapping(value = "/search/amounts")
    public List<RegistrationAmountSearchResult> search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                           @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                       @RequestBody RegistrationAmountSearchParameters searchParams) {
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchResults results =
            registrationService.searchAmounts(SSL_CLIENT_S_DN_O,
                super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchParameters.class));
        return super.mapSearchResultsToList(results, RegistrationAmountSearchResult.class);
    }
}
