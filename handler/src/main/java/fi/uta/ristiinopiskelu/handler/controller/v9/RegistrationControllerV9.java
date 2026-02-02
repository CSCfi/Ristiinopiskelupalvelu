package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationAmountSearchResult;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.RegistrationStatusAmountSearchResults;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "registrations", description = "Rekisteröintipyynnöt v9")
@RequestMapping("/api/v9/registrations")
@RestController
public class RegistrationControllerV9 {

    @Autowired
    private RegistrationService registrationService;

    @Operation(summary = "Hae rekisteröintipyyntöjen tilojen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "tilojen lukumääriä annetuista verkostoista.", tags = "registrations")
    @PostMapping(value = "/search/status/amounts")
    public RegistrationStatusAmountSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                            @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationStatusAmountSearchParameters searchParams) {
        return registrationService.searchStatusAmounts(SSL_CLIENT_S_DN_O, searchParams);
    }

    @Operation(summary = "Hae rekisteröintipyyntöjen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "lukumääriä annetuilla hakuehdoilla.", tags = "registrations")
    @PostMapping(value = "/search/amounts")
    public List<RegistrationAmountSearchResult> search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                           @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationAmountSearchParameters searchParams) {
        return registrationService.searchAmounts(SSL_CLIENT_S_DN_O, searchParams).getResults();
    }
}
