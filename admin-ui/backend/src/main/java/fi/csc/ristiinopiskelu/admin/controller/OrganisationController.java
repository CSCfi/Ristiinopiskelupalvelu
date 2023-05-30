package fi.csc.ristiinopiskelu.admin.controller;

import fi.csc.ristiinopiskelu.admin.dto.CreateOrUpdateOrganisationDTO;
import fi.csc.ristiinopiskelu.admin.dto.OrganisationDTO;
import fi.csc.ristiinopiskelu.admin.services.OrganisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping("/api/organisations")
@RestController
public class OrganisationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganisationController.class);

    @Autowired
    private OrganisationService organisationService;

    @PreAuthorize("isAuthenticated() and hasRole('SUPERUSER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public OrganisationDTO createOrganisation(@RequestBody CreateOrUpdateOrganisationDTO organisation) {
        return organisationService.create(organisation);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping(value = "/{organisationId}", produces = APPLICATION_JSON_VALUE)
    public OrganisationDTO updateOrganisation(@PathVariable("organisationId") String organisationId, @RequestBody CreateOrUpdateOrganisationDTO organisation) {
        return organisationService.update(organisationId, organisation);
    }

    @PreAuthorize("isAuthenticated() and hasRole('SUPERUSER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{organisationId}")
    public void deleteOrganisation(@PathVariable("organisationId") String organisationId) {
        organisationService.deleteById(organisationId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<OrganisationDTO> getOrganisations() {
       return organisationService.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/schemaversions", produces = APPLICATION_JSON_VALUE)
    public List<Integer> getSchemaVersions() {
        return organisationService.getSchemaVersions();
    }
}
