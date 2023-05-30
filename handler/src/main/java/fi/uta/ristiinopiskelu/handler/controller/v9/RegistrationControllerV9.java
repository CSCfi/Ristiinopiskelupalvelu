package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration.*;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "registrations", description = "Rekisteröintipyynnöt v9")
@RequestMapping("/api/v9/registrations")
@RestController
public class RegistrationControllerV9 {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationControllerV9.class);

    @Autowired
    private RegistrationService registrationService;

    @Operation(summary = "Hae rekisteröintipyyntöjen tilojen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "tilojen lukumääriä annetuista verkostoista.", tags = "registrations")
    @RequestMapping(value = "/search/status/amounts", method = RequestMethod.POST)
    public RegistrationStatusAmountSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                     @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationStatusAmountSearchParameters searchParams) {
        return registrationService.searchStatusAmounts(SSL_CLIENT_S_DN_O, searchParams);
    }

    @Operation(summary = "Hae rekisteröintipyyntöjen lukumääriä", description = "Operaatio hakee reksteröintipyyntöjen " +
        "lukumääriä annetuilla hakuehdoilla.", tags = "registrations")
    @RequestMapping(value = "/search/amounts", method = RequestMethod.POST)
    public List<RegistrationAmountSearchResult> search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody RegistrationAmountSearchParameters searchParams) {
        return registrationService.searchAmounts(SSL_CLIENT_S_DN_O, searchParams).getResults();
    }
}
