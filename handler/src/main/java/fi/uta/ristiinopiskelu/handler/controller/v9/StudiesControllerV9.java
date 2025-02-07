package fi.uta.ristiinopiskelu.handler.controller.v9;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CourseUnitReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.courseunit.CourseUnitSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.InternalStudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studymodule.StudyModuleSearchParameters;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.impl.processor.StudiesSearchResultsConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Tag(name = "studies", description = "Tarjonta v9")
@RequestMapping("/api/v9/studies")
@RestController
public class StudiesControllerV9 {

    private static final Logger logger = LoggerFactory.getLogger(StudiesControllerV9.class);

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private StudyModuleService studyModuleService;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private StudiesService studiesService;

    @Operation(summary = "Hae opintojaksoja", description = "Operaatio hakee opintojaksoja. Jos tunnisteita ei ole annettu, palautetaan kaikki. " +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/courseunits", method = RequestMethod.GET)
    public List<CourseUnitReadDTO> findCourseUnits(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                            @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                   @Parameter(description = "Opintojakson tunniste") @RequestParam(value = "courseUnitId", required = false) String courseUnitId,
                                                   @Parameter(description = "Opintojakson tunnistekoodi") @RequestParam(value = "courseUnitIdentifierCode", required = false) String courseUnitIdentifierCode,
                                                   @Parameter(description = "Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                   @Parameter(description = "Opintojaksojen tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                   @Parameter(description = "Palautetaanko inaktiiviset") @RequestParam(defaultValue = "true", required = false) boolean includeInactive,
                                                   @Parameter(description = "Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                   @Parameter(description = "Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        return courseUnitService.search(SSL_CLIENT_S_DN_O, new CourseUnitSearchParameters(courseUnitId, courseUnitIdentifierCode,
            organizingOrganisationId, statuses, includeInactive, page, pageSize)).getResults();
    }

    @Operation(summary = "Hae opintojakson toteutuksia", description = "Operaatio hakee opintojakson toteutuksia opintojakson tunnisteiden perusteella." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/courseunits/{courseUnitId}/realisations", method = RequestMethod.GET)
    public List<RealisationReadDTO> findCourseUnitRealisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                               @Parameter(description ="Opintojakson tunniste") @PathVariable("courseUnitId") String courseUnitId,
                                                               @Parameter(description ="Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                               @Parameter(description ="Toteutusten tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                               @Parameter(description ="Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                               @Parameter(description ="Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        RealisationSearchParameters searchParameters = new RealisationSearchParameters();
        searchParameters.setIncludeInactive(true);
        searchParameters.setOngoingEnrollment(false);
        searchParameters.setIncludeOwn(true);
        searchParameters.setCourseUnitReferences(Collections.singletonList(new CourseUnitReference(courseUnitId, organizingOrganisationId)));
        searchParameters.setStatuses(statuses);
        searchParameters.setPage(page);
        searchParameters.setPageSize(pageSize);
        return new ArrayList<>(realisationService.search(SSL_CLIENT_S_DN_O, searchParameters).getResults());
    }

    @Operation(summary = "Hae opintokokonaisuuksia", description = "Operaatio hakee opintokokonaisuuksia. Jos tunnisteita ei ole " +
        "annettu, palautetaan kaikki. Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags = "studies")
    @RequestMapping(value = "/studymodules", method = RequestMethod.GET)
    public List<StudyModuleReadDTO> findStudyModules(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                     @Parameter(description = "Opintokokonaisuuden tunniste") @RequestParam(value = "studyModuleId", required = false) String studyModuleId,
                                                     @Parameter(description = "Opintokokonaisuuden tunnistekoodi") @RequestParam(value = "studyModuleIdentifierCode", required = false) String studyModuleIdentifierCode,
                                                     @Parameter(description = "Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                     @Parameter(description = "Opintokokonaisuuksien tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                     @Parameter(description = "Palautetaanko inaktiiviset") @RequestParam(defaultValue = "true", required = false) boolean includeInactive,
                                                     @Parameter(description = "Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                     @Parameter(description = "Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        return studyModuleService.search(SSL_CLIENT_S_DN_O, new StudyModuleSearchParameters(studyModuleId, studyModuleIdentifierCode,
            organizingOrganisationId, statuses, includeInactive, page, pageSize)).getResults();
    }

    @Operation(summary = "Hae toteutus", description = "Operaatio palauttaa toteutuksen, joka löytyy annetuilla tunnisteilla. Jos tunnisteita ei ole annettu, palautetaan kaikki." +
            " Jos haettava toteutus ei ole suorittavan organisaation oma, pitää toteutuksen kuulua verkostoon, johon hakeva organisaatio kuuluu ja osallistuminen verkostoon on voimassa." +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/realisations", method = RequestMethod.GET)
    public List<RealisationReadDTO> findRealisation(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                        @Parameter(description ="Toteutuksen tunniste") @RequestParam(value = "realisationId", required = false) String realisationId,
                                                        @Parameter(description ="Toteutuksen tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                        @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                        @RequestParam(defaultValue = "0", required = false) Integer page,
                                                        @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        return new ArrayList<>(realisationService.searchByIds(SSL_CLIENT_S_DN_O, realisationId, organizingOrganisationId,
                statuses, PageRequest.of(page, pageSize)).getResults());
    }

    @Operation(summary = "Hae toteutuksia", description = "Operaatio hakee toteutuksia annetuilla hakuehdoilla. Hakee oletuksena toteutuksia, joissa ilmoittautumisaika on meneillään," +
            " verkostoliitos on voimassa, verkosto on voimassa sekä organisaation liittyminen on voimassa verkostossa ja jotka eivät ole kyselyn tehneen organisaation omia toteutuksia." +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/realisations/search", method = RequestMethod.POST)
    public List<RealisationReadDTO> findRealisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                              @RequestBody RealisationSearchParameters searchParameters) {
        return new ArrayList<>(realisationService.search(SSL_CLIENT_S_DN_O, searchParameters).getResults());
    }

    @Operation(summary = "Hae tarjontaa", description = "Operaatio hakee tarjontaa annetuilla hakuehdoilla. Palautttaa opintoelementtejä eli opintojaksoja, opintokokonaisuuksia ja tutkintoja." +
            " Palauttaa oletuksena kaikkia elementtejä, joissa verkosto on voimassa ja organisaation liittyminen on voimassa verkostossa, jotka ovat aktiivisia ja jotka eivät ole kyselyn tehneen organisaation omia. " +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public StudiesSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                               @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudiesSearchParameters searchParams) {
        InternalStudiesSearchResults results = studiesService.search(SSL_CLIENT_S_DN_O, searchParams);
        results.getAggregations().removeIf(aggregation -> aggregation.getName().equals("realisationTeachingLanguages"));

        // convert aggs to deprecated Simple* aggregation classes for now, in order to maintain API compatability
        return new StudiesSearchResultsConverter().convert(results);
    }
}
