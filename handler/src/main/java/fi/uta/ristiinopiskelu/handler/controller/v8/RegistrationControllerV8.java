package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration.*;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "registrations", description = "Rekisteröintipyynnöt v8")
@RequestMapping("/api/v8/registrations")
@RestController
public class RegistrationControllerV8 extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationControllerV8.class);

    @Autowired
    private RegistrationService registrationService;

    @Operation(summary = "Hae rekisteröintipyyntöjen tilojen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "tilojen lukumääriä annetuista verkostoista.", tags = "registrations")
    @RequestMapping(value = "/search/status/amounts", method = RequestMethod.POST)
    public RegistrationStatusAmountSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                            @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationStatusAmountSearchParameters searchParams) {
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchResults result =
            registrationService.searchStatusAmounts(SSL_CLIENT_S_DN_O,
                super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchParameters.class));
        return super.mapToDto(result, RegistrationStatusAmountSearchResults.class);
    }

    @Operation(summary = "Hae rekisteröintipyyntöjen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen lukumääriä " +
        "annetuilla hakuehdoilla.", tags = "registrations")
    @RequestMapping(value = "/search/amounts", method = RequestMethod.POST)
    public List<RegistrationAmountSearchResult> search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationAmountSearchParameters searchParams) {
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchResults results =
            registrationService.searchAmounts(SSL_CLIENT_S_DN_O,
                super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchParameters.class));
        return super.mapSearchResultsToList(results, RegistrationAmountSearchResult.class);
    }
}
