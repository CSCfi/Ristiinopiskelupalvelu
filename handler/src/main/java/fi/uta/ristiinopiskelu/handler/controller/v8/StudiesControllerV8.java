package fi.uta.ristiinopiskelu.handler.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CourseUnitReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.CourseUnitSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.StudyModuleSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudiesRestSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyElementRestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.AggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.MultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.SingleBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.handler.controller.AbstractController;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "studies", description = "Tarjonta v8")
@RequestMapping("/api/v8/studies")
@RestController
public class StudiesControllerV8 extends AbstractController {

    private static final Logger logger = LoggerFactory.getLogger(StudiesControllerV8.class);

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
    public List<CourseUnitRestDTO> findCourseUnits(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                       @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                   @Parameter(description = "Opintojakson tunniste") @RequestParam(value = "courseUnitId", required = false) String courseUnitId,
                                                   @Parameter(description = "Opintojakson tunnistekoodi") @RequestParam(value = "courseUnitIdentifierCode", required = false) String courseUnitIdentifierCode,
                                                   @Parameter(description = "Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                   @Parameter(description = "Opintojaksojen tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                   @Parameter(description = "Palautetaanko inaktiiviset") @RequestParam(defaultValue = "true", required = false) boolean includeInactive,
                                                   @Parameter(description = "Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                   @Parameter(description = "Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.courseunit.CourseUnitSearchParameters searchParams = super.mapToDto(
            new CourseUnitSearchParameters(courseUnitId, courseUnitIdentifierCode, organizingOrganisationId, statuses,
                includeInactive, page, pageSize), fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.courseunit.CourseUnitSearchParameters.class);

        return super.mapSearchResultsToList(courseUnitService.search(SSL_CLIENT_S_DN_O, searchParams), CourseUnitRestDTO.class);
    }

    @Operation(summary = "Hae opintojakson toteutuksia", description = "Operaatio hakee opintojakson toteutuksia opintojakson tunnisteiden perusteella." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/courseunits/{courseUnitId}/realisations", method = RequestMethod.GET)
    public List<Realisation> findCourseUnitRealisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                        @Parameter(description ="Opintojakson tunniste") @PathVariable("courseUnitId") String courseUnitId,
                                                        @Parameter(description ="Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                        @Parameter(description ="Toteutusten tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                        @Parameter(description ="Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                        @Parameter(description ="Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        RealisationSearchParameters searchParams = new RealisationSearchParameters();
        searchParams.setIncludeInactive(true);
        searchParams.setOngoingEnrollment(false);
        searchParams.setIncludeOwn(true);
        searchParams.setCourseUnitReferences(Collections.singletonList(new CourseUnitReference(courseUnitId, organizingOrganisationId)));
        searchParams.setStatuses(statuses);
        searchParams.setPage(page);
        searchParams.setPageSize(pageSize);

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters currentSearchParams = super.mapToDto(
            searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters.class);

        return super.mapSearchResultsToList(realisationService.search(SSL_CLIENT_S_DN_O, currentSearchParams), Realisation.class);
    }

    @Operation(summary = "Hae opintokokonaisuuksia", description = "Operaatio hakee opintokokonaisuuksia. Jos tunnisteita ei ole " +
        "annettu, palautetaan kaikki. Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags = "studies")
    @RequestMapping(value = "/studymodules", method = RequestMethod.GET)
    public List<StudyModuleRestDTO> findStudyModules(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                     @Parameter(description = "Opintokokonaisuuden tunniste") @RequestParam(value = "studyModuleId", required = false) String studyModuleId,
                                                     @Parameter(description = "Opintokokonaisuuden tunnistekoodi") @RequestParam(value = "studyModuleIdentifierCode", required = false) String studyModuleIdentifierCode,
                                                     @Parameter(description = "Opintojakson tarjoavan organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                     @Parameter(description = "Opintokokonaisuuksien tilat") @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                     @Parameter(description = "Palautetaanko inaktiiviset") @RequestParam(defaultValue = "true", required = false) boolean includeInactive,
                                                     @Parameter(description = "Sivunumero") @RequestParam(defaultValue = "0", required = false) Integer page,
                                                     @Parameter(description = "Sivun koko") @RequestParam(defaultValue = "100", required = false) Integer pageSize) {

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studymodule.StudyModuleSearchParameters searchParams = super.mapToDto(
            new StudyModuleSearchParameters(studyModuleId, studyModuleIdentifierCode, organizingOrganisationId, statuses,
                includeInactive, page, pageSize), fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studymodule.StudyModuleSearchParameters.class);

        return super.mapSearchResultsToList(studyModuleService.search(SSL_CLIENT_S_DN_O, searchParams), StudyModuleRestDTO.class);
    }

    @Operation(summary = "Hae toteutus", description =  "Operaatio palauttaa toteutuksen, joka löytyy annetuilla tunnisteilla." +
            " Jos haettava toteutus ei ole suorittavan organisaation oma, pitää toteutuksen kuulua verkostoon, johon hakeva organisaatio kuuluu ja osallistuminen verkostoon on voimassa." +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/realisations", method = RequestMethod.GET)
    public List<Realisation> findRealisation(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                                        @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                                        @Parameter(description = "Toteutuksen tunniste") @RequestParam(value = "realisationId", required = false) String realisationId,
                                                        @Parameter(description = "Opintojakson tarjoava organisaation tilastokeskuskoodi") @RequestParam(value = "organizingOrganisationId", required = false) String organizingOrganisationId,
                                                        @RequestParam(defaultValue = "ACTIVE", required = false) List<StudyStatus> statuses,
                                                        @RequestParam(defaultValue = "0", required = false) Integer page,
                                                        @RequestParam(defaultValue = "100", required = false) Integer pageSize) {
        List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus> currentVersionStatuses = statuses.stream()
            .map(st -> fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.valueOf(st.name())).collect(Collectors.toList());

        return super.mapSearchResultsToList(realisationService.searchByIds(SSL_CLIENT_S_DN_O, realisationId,
            organizingOrganisationId, currentVersionStatuses, PageRequest.of(page, pageSize)), Realisation.class);
    }

    @Operation(summary = "Hae toteutuksia", description = "Operaatio hakee toteutuksia annetuilla hakuehdoilla. Hakee oletuksena toteutuksia, joissa ilmoittautumisaika on meneillään," +
            " verkostoliitos on voimassa, verkosto on voimassa sekä organisaation liittyminen on voimassa verkostossa ja jotka eivät ole kyselyn tehneen organisaation omia toteutuksia." +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/realisations/search", method = RequestMethod.POST)
    public List<Realisation> findRealisations(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                              @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O,
                                              @RequestBody RealisationSearchParameters searchParameters) {

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters currentSearchParams =
            super.mapToDto(searchParameters, fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters.class);

        return super.mapSearchResultsToList(realisationService.search(SSL_CLIENT_S_DN_O, currentSearchParams), Realisation.class);
    }

    @Operation(summary = "Hae tarjontaa", description = "Operaatio hakee tarjontaa annetuilla hakuehdoilla. Palautttaa opintoelementtejä eli opintojaksoja, opintokokonaisuuksia ja tutkintoja." +
            " Palauttaa oletuksena kaikkia elementtejä, joissa verkosto on voimassa ja organisaation liittyminen on voimassa verkostossa, jotka ovat aktiivisia ja jotka eivät ole kyselyn tehneen organisaation omia. " +
            " Jos yhtään toteutusta ei hakuehdoilla löydy, niin palautetaan tyhjä lista." +
            " Operaatio käyttää sivutusta ja palauttaa oletuksena 100 ensimmäistä tulosta.", tags ="studies")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public StudiesRestSearchResults search(@Parameter(name = "SSL_CLIENT_S_DN_O", description = "Sertifikaatista tuleva organisaatiotunnus (ei määritettävissä)", hidden = true)
                                               @RequestHeader("SSL_CLIENT_S_DN_O") String SSL_CLIENT_S_DN_O, @RequestBody StudiesSearchParameters searchParams) {

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters currentSearchParams =
            super.mapToDto(searchParams, fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters.class);

        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.InternalStudiesSearchResults internalResults = studiesService.search(SSL_CLIENT_S_DN_O, currentSearchParams);
        fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults results = new StudiesSearchResultsConverter().convert(internalResults);

        return new StudiesRestSearchResults(
            results.getResults().stream().map(this::mapStudyElement).collect(Collectors.toList()),
            results.getAggregations().stream().map(this::mapAggregation).filter(agg -> !agg.getName().equals("studyElementsByRealisationTeachingLanguages")).collect(Collectors.toList()),
            results.getTotalHits());
    }

    private StudyElementRestDTO mapStudyElement(AbstractStudyElementReadDTO entity) {
        if(entity.getType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT) {
            return super.mapToDto(entity, fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO.class);
        } else if (entity.getType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE) {
            return super.mapToDto(entity, fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyModuleRestDTO.class);
        } else if (entity.getType() == fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.DEGREE) {
            return super.mapToDto(entity, fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.DegreeRestDTO.class);
        } else {
            throw new IllegalStateException("Unknown StudyElement type:" + entity.getClass());
        }
    }

    private AggregationDTO mapAggregation(SimpleAggregationDTO dto) {
        switch (dto.getType()) {
            case MULTI: return super.mapToDto(dto, MultiBucketAggregationDTO.class);
            case SINGLE: return super.mapToDto(dto, SingleBucketAggregationDTO.class);
            default: throw new IllegalStateException("Unknown aggregation type: " + dto.getType());
        }
    }
}
