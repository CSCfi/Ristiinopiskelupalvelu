package fi.uta.ristiinopiskelu.handler.controller;

import fi.uta.ristiinopiskelu.handler.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "internal", description = "Internal API")
@RequestMapping("/internal/api")
@RestController
public class InternalController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "Pysäytä viestiliikenne", description = "Operaatio pysäyttää käsittelijän viestiliikenteen.", tags = "messaging")
    @RequestMapping(method = RequestMethod.PUT, path = "/messaging/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendMessageProcessing() {
        messageService.suspendMessageProcessing();
    }

    @Operation(summary = "Käynnistä viestiliikenne", description = "Operaatio käynnistää käsittelijän viestiliikenteen.", tags = "messaging")
    @RequestMapping(method = RequestMethod.PUT, path = "/messaging/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resumeMessageProcessing() {
        messageService.resumeMessageProcessing();
    }

    @Operation(summary = "Tarkista viestiliikenteen tila", description = "Operaatio palauttaa totuusarvon viestiliikenteen tilasta " +
        "(true = käynnissä, false = pysäytetty/pysähtymässä).", tags = "messaging")
    @RequestMapping(method = RequestMethod.GET, path = "/messaging/status")
    public boolean isMessageProcessingActive() {
        return !messageService.isMessageProcessingSuspended();
    }
}
