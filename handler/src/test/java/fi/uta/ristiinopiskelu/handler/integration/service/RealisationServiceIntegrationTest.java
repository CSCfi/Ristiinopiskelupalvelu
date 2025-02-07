package fi.uta.ristiinopiskelu.handler.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CourseUnitReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class RealisationServiceIntegrationTest {

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private List<StudyElementReference> courseUnitReferences;
    private List<RealisationEntity> allExistingRealisations;
    private List<RealisationEntity> validExistingRealisations;
    private List<RealisationEntity> similarNameRealisations;
    private List<RealisationEntity> noOngoingEnrollmentRealisations;
    private List<RealisationEntity> validInPastRealisations;
    private List<NetworkEntity> existingNetworks;
    private List<NetworkEntity> validNetworks;
    private List<NetworkEntity> invalidValidityNetworks;

    @BeforeEach
    public void beforeAll() {
        List<NetworkOrganisation> orgs = new ArrayList<>();
        List<String> orgIds = Arrays.asList("TUNI", "UEF", "HAAGAH", "METROP", "JYU");
        for(String orgId : orgIds) {
            orgs.add(new NetworkOrganisation(orgId, false, new Validity(Validity.ContinuityEnum.FIXED,
                    OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusYears(1))));
        }

        invalidValidityNetworks = new ArrayList<>();

        invalidValidityNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("INVALID-VALIDITY-PAST1", null, orgs,
                new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusYears(1)), true)));
        invalidValidityNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("INVALID-VALIDITY-FUTURE1", null, orgs,
                new Validity(Validity.ContinuityEnum.INDEFINITELY, OffsetDateTime.now().plusYears(1), OffsetDateTime.now().minusYears(1)), true)));
        invalidValidityNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("INVALID-VALIDITY-FUTURE2", null, orgs,
                new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2)), true)));
        invalidValidityNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("INVALID-VALIDITY-STATUS-FALSE", null, orgs,
                new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusYears(1)), false)));

        validNetworks = new ArrayList<>();

        validNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("CN1", new LocalisedString("Verkosto 1", null, null), orgs,
                DtoInitializer.getFixedValidity(OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusYears(1)), true)));

        validNetworks.add(networkRepository.save(EntityInitializer.getNetworkEntity("CN2", new LocalisedString("Verkosto 2", null, null), orgs,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true)));

        existingNetworks = new ArrayList<>();
        existingNetworks.addAll(invalidValidityNetworks);
        existingNetworks.addAll(validNetworks);

        courseUnitReferences = new ArrayList<>();
        // 0-4 TUNI refs
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-1",  "TUNI", "AI1"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-2",  "TUNI", "AI2"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-7", "TUNI"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-6", "TUNI", "AI6"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-12", "TUNI"));

        // 5-7 HAAGAH refs
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-3", "HAAGAH", "AI3"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-9",   "HAAGAH"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-13",  "HAAGAH"));

        // 8-9 JYU refs
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-4", "JYU", "AI4"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-10", "JYU"));

        // 10-12 METROP refs
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-5", "METROP", "AI5"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-11", "METROP"));
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-14", "METROP"));

        // 13 UEF ref
        courseUnitReferences.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-8",  "UEF"));

        createRealisations();
    }

    private void createRealisations() {
        validExistingRealisations = new ArrayList<>();
        allExistingRealisations = new ArrayList<>();
        similarNameRealisations = new ArrayList<>();
        noOngoingEnrollmentRealisations = new ArrayList<>();
        validInPastRealisations = new ArrayList<>();

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R1", "RCODE1", "TUNI",
                "Toteutus 1", "Realisation 1", "Realisering 1", courseUnitReferences.subList(0,1), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R2", "RCODE2", "TUNI",
                "Toteutus 2", "Realisation 2", "Realisering 2", courseUnitReferences.subList(2,4), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R3", "RCODE3", "HAAGAH",
                "Toteutus 3", "Realisation 3", "Realisering 3", courseUnitReferences.subList(5,7), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R4", "RCODE4", "UEF",
                "Toteutus 4", "Realisation 4", "Realisering 4", Arrays.asList(courseUnitReferences.get(13)), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R5", "RCODE5", "JYU",
                "Toteutus 5", "Realisation 5", "Realisering 5", courseUnitReferences.subList(8,9), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R6", "RCODE6", "METROP",
                "Toteutus 6", "Realisation 6", "Realisering 6", courseUnitReferences.subList(10,12), OffsetDateTime.now().minusDays(10), null,
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R7", "RCODE7", "METROP",
                "Toteutus 7", "Realisation 7", "Realisering 7", Arrays.asList(courseUnitReferences.get(10)), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R8", "RCODE8", "UEF",
                "Toteutus 8", "Realisation 8", "Realisering 8", Arrays.asList(courseUnitReferences.get(13)), OffsetDateTime.now().minusDays(10), null,
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R9", "RCODE9", "HAAGAH",
                "Toteutus 9", "Realisation 9", "Realisering 9", Arrays.asList(courseUnitReferences.get(6)), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        validExistingRealisations.add(generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R10", "RCODE10", "METROP",
                "Toteutus 10", "Realisation 10", "Realisering 10", courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1)));

        allExistingRealisations.addAll(validExistingRealisations);

        RealisationEntity futureEnrollment = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R11", "RCODE11", "METROP",
                "Toteutus 11", "Realisation 11", "Realisering 11", courseUnitReferences.subList(11,12), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationEntity pastEnrollment = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R12", "RCODE12", "METROP",
                "Toteutus 12", "Realisation 12", "Realisering 12", courseUnitReferences.subList(10,12), OffsetDateTime.now().minusMonths(1), OffsetDateTime.now().minusDays(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationEntity noEnrollmentTimes = generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), "R13", "RCODE13", "METROP",
                "Toteutus 13", "Realisation 13", "Realisering 13", Arrays.asList(courseUnitReferences.get(10)), null, null,
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        noOngoingEnrollmentRealisations.add(futureEnrollment);
        noOngoingEnrollmentRealisations.add(pastEnrollment);
        noOngoingEnrollmentRealisations.add(noEnrollmentTimes);

        allExistingRealisations.addAll(noOngoingEnrollmentRealisations);

        RealisationEntity similarNameRealisation = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R14", "RCODE14", "METROP", "ToteKAHVIutus", "RealiCOFFEEsation", "RealiKAFFEsering",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationEntity similarNameRealisation2 = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R15", "RCODE15", "METROP", "Toteutus 10 kahvi", "Realisation coffee", "Realisering kaffe",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationEntity similarNameRealisation3 = generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R16", "RCODE16", "METROP", "KahviToteutus 10", "CoffeeRealisation", "KaffeRealisering",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationEntity similarNameRealisation4 = generateRealisation("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R17", "RCODE17", "METROP", "Toteutus kahvI asd", "Realisation coffeE asd", "Realisering kaffE asd",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        similarNameRealisations.add(similarNameRealisation);
        similarNameRealisations.add(similarNameRealisation2);
        similarNameRealisations.add(similarNameRealisation3);
        similarNameRealisations.add(similarNameRealisation4);

        validExistingRealisations.addAll(similarNameRealisations);
        allExistingRealisations.addAll(similarNameRealisations);

        RealisationEntity validInPastRealisation = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R18", "RCODE18", "METROP", "Toteutus 18", "Realisation 18", "Realisering 18",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(1), LocalDate.now().minusDays(1));

        RealisationEntity validInPastRealisation2 = generateRealisation("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "R19", "RCODE19", "METROP", "Toteutus 18", "Realisation 19", "Realisering 19",
                courseUnitReferences.subList(11,12), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));

        validInPastRealisations.addAll(Arrays.asList(validInPastRealisation, validInPastRealisation2));
        allExistingRealisations.addAll(validInPastRealisations);
    }


    @Test
    public void testSearchOrganisationFiltering_shouldReturnRealisationsOrganiserNotRequestingOrganisation() {
        String requestingOrganisation = "TUNI";

        List<RealisationEntity> shouldReturnRealisations = allExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .collect(Collectors.toList());

        RealisationSearchParameters params = new RealisationSearchParameters();
        params.setOngoingEnrollment(false);

        RealisationSearchResults result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());
    }

    @Test
    public void testSearchEnrollmentFiltering_shouldReturnRealisationsWithOngoingEnrollment() {
        String requestingOrganisation = "TUNI";

        List<RealisationEntity> shouldReturnRealisations = allExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .filter(r -> noOngoingEnrollmentRealisations.stream().noneMatch(vr ->
                        vr.getRealisationId().equals(r.getRealisationId())
                                && vr.getRealisationIdentifierCode().equals(r.getRealisationIdentifierCode())
                ))
                .collect(Collectors.toList());

        RealisationSearchParameters params = new RealisationSearchParameters();
        params.setOngoingEnrollment(true);

        RealisationSearchResults result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());
    }

    @Test
    public void testSearchCourseUnitReferenceFiltering_shouldReturnOnlyRealisationsWithGivenReference() {
        String requestingOrganisation = "TUNI";

        CourseUnitReference assessmentItemQueryReference = new CourseUnitReference(
                courseUnitReferences.get(10).getReferenceIdentifier(),
                courseUnitReferences.get(10).getReferenceOrganizer());

        CourseUnitReference queryReference = new CourseUnitReference(
                courseUnitReferences.get(13).getReferenceIdentifier(),
                courseUnitReferences.get(13).getReferenceOrganizer());

        List<RealisationEntity> shouldReturnRealisations = validExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .filter(r -> r.getStudyElementReferences().stream()
                        .anyMatch(ref ->
                                (ref.getReferenceIdentifier().equals(assessmentItemQueryReference.getCourseUnitId())
                                    && ref.getReferenceOrganizer().equals(assessmentItemQueryReference.getOrganizingOrganisationId()))
                            || (ref.getReferenceIdentifier().equals(queryReference.getCourseUnitId())
                                    && ref.getReferenceOrganizer().equals(queryReference.getOrganizingOrganisationId()))))
                .collect(Collectors.toList());

        RealisationSearchParameters params = new RealisationSearchParameters();
        params.setCourseUnitReferences(Arrays.asList(assessmentItemQueryReference, queryReference));
        params.setOngoingEnrollment(true);

        RealisationSearchResults result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());
        assertTrue(result.getResults().stream().allMatch(r -> r.getStudyElementReferences().stream()
                        .anyMatch(ref ->
                                (ref.getReferenceIdentifier().equals(assessmentItemQueryReference.getCourseUnitId())
                                    && ref.getReferenceOrganizer().equals(assessmentItemQueryReference.getOrganizingOrganisationId()))
                                || (ref.getReferenceIdentifier().equals(queryReference.getCourseUnitId())
                                    && ref.getReferenceOrganizer().equals(queryReference.getOrganizingOrganisationId())))));

        // Test only id ref query working
        CourseUnitReference onlyIdRef = new CourseUnitReference(courseUnitReferences.get(5).getReferenceIdentifier(), null);

        shouldReturnRealisations = validExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .filter(r -> r.getStudyElementReferences().stream()
                        .anyMatch(ref -> (ref.getReferenceIdentifier().equals(onlyIdRef.getCourseUnitId()))))
                .collect(Collectors.toList());

        params = new RealisationSearchParameters();
        params.setCourseUnitReferences(Collections.singletonList(onlyIdRef));
        params.setOngoingEnrollment(true);

        result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());
        assertTrue(result.getResults().stream().allMatch(r -> r.getStudyElementReferences().stream()
                .anyMatch(ref -> (ref.getReferenceIdentifier().equals(onlyIdRef.getCourseUnitId())))));

        // Test only organisation ref query working
        CourseUnitReference onlyOrganisationRef = new CourseUnitReference(null, "HAAGAH");
        CourseUnitReference onlyOrganisationRef2 = new CourseUnitReference(null, "JYU");

        shouldReturnRealisations = validExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .filter(r -> r.getStudyElementReferences().stream()
                        .anyMatch(ref -> (ref.getReferenceOrganizer().equals("HAAGAH") || ref.getReferenceOrganizer().equals("JYU"))))
                .collect(Collectors.toList());

        params = new RealisationSearchParameters();
        params.setCourseUnitReferences(Arrays.asList(onlyOrganisationRef, onlyOrganisationRef2));
        params.setOngoingEnrollment(true);

        result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());
        assertTrue(result.getResults().stream().allMatch(r -> r.getStudyElementReferences().stream()
                .anyMatch(ref -> (ref.getReferenceOrganizer().equals("HAAGAH") || ref.getReferenceOrganizer().equals("JYU")))));
    }

    @Test
    public void testSearchNetworkFiltering_shouldReturnOnlyRealisationsWithGivenNetwork() {
        String requestingOrganisation = "TUNI";

        List<RealisationEntity> realisationsWithInvalidNetwork = new ArrayList<>();
        int i = 0;
        for(NetworkEntity invalidNetwork : invalidValidityNetworks) {
            RealisationEntity invalidRealisationEntity = generateRealisation(invalidNetwork.getId(), LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                    "R" + (99 + i), "RCODE" + (99 + i), "METROP", "Toteutus voimassaolemattomalla verkostolla", "Invalid network", null,
                    courseUnitReferences.subList(0,1), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1), LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));
            realisationsWithInvalidNetwork.add(invalidRealisationEntity);
        }

        RealisationSearchParameters params = new RealisationSearchParameters();
        params.setNetworkIdentifiers(Collections.singletonList("CN1"));
        params.setIncludePast(false);

        List<RealisationEntity> shouldReturnRealisations = validExistingRealisations.stream()
                .filter(r -> !r.getOrganizingOrganisationId().equals(requestingOrganisation))
                .filter(r -> r.getCooperationNetworks().stream().anyMatch(cn -> params.getNetworkIdentifiers().contains(cn.getId())))
                .collect(Collectors.toList());

        RealisationSearchResults result = realisationService.search(requestingOrganisation, params);
        assertEquals(shouldReturnRealisations.size(), result.getResults().size());

        assertTrue(result.getResults().stream().allMatch(r ->
                r.getCooperationNetworks().stream().anyMatch(cn -> params.getNetworkIdentifiers().contains(cn.getId()))));

        assertTrue(result.getResults().stream().noneMatch(res -> realisationsWithInvalidNetwork.stream()
                .anyMatch(invalidNetwork -> invalidNetwork.getRealisationId().equals(res.getRealisationId())
                && invalidNetwork.getRealisationIdentifierCode().equals(res.getRealisationIdentifierCode()))));
    }

    @Test
    public void testSearchNetworkFiltering_shouldNotReturnRealisationsInWhichNetworkOrganisationNotValid() {
        String organisationRunningQuery = "TUNI";

        Validity futureValidity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2));
        Validity pastValidity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().minusYears(2), OffsetDateTime.now().minusYears(1));
        Validity futureValidity2 = new Validity(Validity.ContinuityEnum.INDEFINITELY, OffsetDateTime.now().plusYears(1), null);

        // Test future validity
        NetworkEntity networkEntity = validNetworks.get(0);
        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(futureValidity);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        RealisationSearchParameters searchParameters = new RealisationSearchParameters();
        searchParameters.setIncludeOwn(true);
        RealisationSearchResults result = this.realisationService.search(organisationRunningQuery, searchParameters);

        List<RealisationEntity> expectedResults = validExistingRealisations.stream()
                .filter(se -> se.getCooperationNetworks().stream().noneMatch(cn -> cn.getId().equals(networkEntity.getId())))
                .collect(Collectors.toList());

        assertEquals(expectedResults.size(), result.getResults().size());
        assertTrue(result.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));

        // Test past validity
        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(pastValidity);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        result = this.realisationService.search(organisationRunningQuery, searchParameters);
        assertEquals(expectedResults.size(), result.getResults().size());
        assertTrue(result.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));

        // Test other future validity
        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(futureValidity2);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        result = this.realisationService.search(organisationRunningQuery, searchParameters);
        assertEquals(expectedResults.size(), result.getResults().size());
        assertTrue(result.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));
    }
    
    @Test
    public void testSearchNameFiltering_shouldReturnOnlyRealisationsWhichNameContainsGivenWord() {
        String requestingOrganisation = "TUNI";

        RealisationSearchParameters fiParams = new RealisationSearchParameters();
        fiParams.setQuery("kahvi");
        fiParams.setLanguage(Language.FI);

        RealisationSearchResults result = realisationService.search(requestingOrganisation, fiParams);
        assertEquals(similarNameRealisations.size(), result.getResults().size());
        assertTrue(result.getResults().stream().allMatch(
                r -> r.getName().getValue("fi").toLowerCase().contains(fiParams.getQuery().toLowerCase())));

        RealisationSearchParameters enParams = new RealisationSearchParameters();
        enParams.setQuery("COFFEE");
        enParams.setLanguage(Language.EN);

        RealisationSearchResults enResult = realisationService.search(requestingOrganisation, fiParams);
        assertEquals(similarNameRealisations.size(), enResult.getResults().size());
        assertTrue(enResult.getResults().stream().allMatch(
                r -> r.getName().getValue("en").toLowerCase().contains(enParams.getQuery().toLowerCase())));

        RealisationSearchParameters svParams = new RealisationSearchParameters();
        svParams.setQuery("kaffE");
        svParams.setLanguage(Language.EN);

        RealisationSearchResults svResult = realisationService.search(requestingOrganisation, fiParams);
        assertEquals(similarNameRealisations.size(), svResult.getResults().size());
        assertTrue(svResult.getResults().stream().allMatch(
                r -> r.getName().getValue("sv").toLowerCase().contains(svParams.getQuery().toLowerCase())));
    }

    @Test
    public void testSearchIdIdentifierCodeOrganizingOrganisationFiltering_shouldReturnRealisationWithGivenIds() {
        String requestingOrganisation = "TUNI";

        RealisationEntity shouldFindRealisation = validExistingRealisations.stream()
                .filter(r -> r.getOrganizingOrganisationId().equals("JYU")).findFirst().get();

        RealisationSearchParameters params = new RealisationSearchParameters(shouldFindRealisation.getRealisationId(),
                shouldFindRealisation.getRealisationIdentifierCode(), shouldFindRealisation.getOrganizingOrganisationId());

        RealisationSearchResults result = realisationService.search(requestingOrganisation, params);
        assertEquals(1, result.getResults().size());
        assertEquals(shouldFindRealisation.getRealisationId(), result.getResults().get(0).getRealisationId());
        assertEquals(shouldFindRealisation.getRealisationIdentifierCode(), result.getResults().get(0).getRealisationIdentifierCode());
        assertTrue(result.getResults().get(0).getOrganisationReferences().stream().anyMatch(
                or -> or.getOrganisation().getOrganisationTkCode().equals(shouldFindRealisation.getOrganizingOrganisationId())
                    && or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER));
    }

    @Test
    public void testSearchByIds_shouldReturnRealisationWithGivenIds() {
        String requestingOrganisation = "TUNI";

        RealisationEntity shouldFindRealisation = validExistingRealisations.stream()
                .filter(r -> r.getOrganizingOrganisationId().equals("JYU")).findFirst().get();

        RealisationSearchResults result = realisationService.searchByIds(requestingOrganisation,
                shouldFindRealisation.getRealisationId(), shouldFindRealisation.getOrganizingOrganisationId(), null, Pageable.unpaged());

        assertEquals(1, result.getResults().size());
        assertEquals(shouldFindRealisation.getRealisationId(), result.getResults().get(0).getRealisationId());
        assertEquals(shouldFindRealisation.getRealisationIdentifierCode(), result.getResults().get(0).getRealisationIdentifierCode());
        assertTrue(result.getResults().get(0).getOrganisationReferences().stream().anyMatch(
                or -> or.getOrganisation().getOrganisationTkCode().equals(shouldFindRealisation.getOrganizingOrganisationId())
                        && or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER));
    }

    @Test
    public void testSearchByIds_shouldReturnRealisationWithGivenIdsAndWithoutNetwork() {
        String requestingOrganisation = "TUNI";

        RealisationEntity realisationEntity = generateRealisation(null, null, null, "R99", "RCODE99", "TUNI",
                "Toteutus ilman verkostoa", "Realisation 99", "Realisering 9", courseUnitReferences.subList(0,1), OffsetDateTime.now().minusDays(10), OffsetDateTime.now().plusMonths(1),
                        LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1));

        RealisationSearchResults result = realisationService.searchByIds(requestingOrganisation,
                realisationEntity.getRealisationId(), realisationEntity.getOrganizingOrganisationId(), null, Pageable.unpaged());

        assertEquals(1, result.getResults().size());
        assertEquals(realisationEntity.getRealisationId(), result.getResults().get(0).getRealisationId());
        assertEquals(realisationEntity.getRealisationIdentifierCode(), result.getResults().get(0).getRealisationIdentifierCode());
        assertTrue(result.getResults().get(0).getOrganisationReferences().stream().anyMatch(
                or -> or.getOrganisation().getOrganisationTkCode().equals(realisationEntity.getOrganizingOrganisationId())
                        && or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER));
    }

    @Test
    public void testSearchCooperationNetworkValidityFiltering_organisationDoesNotBelongToAnyValidNetwork_ShouldNotReturnAnyResults() {
        String organisationRunningQuery = "TUNI";
        String parameterNetworkId = "feb71594-0297-41ac-aae6-fcb8b20383d5";
        String wrongNetworkId = "7d816732-1c9c-49f6-b221-92a9ff3831ac";

        NetworkOrganisation orgTuni = new NetworkOrganisation("NEW_ORGANISATION", false, new Validity(Validity.ContinuityEnum.INDEFINITELY,
                OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusYears(1)));

        NetworkOrganisation orgUef = new NetworkOrganisation("UEF", false, new Validity(Validity.ContinuityEnum.INDEFINITELY,
                OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusYears(1)));

        networkRepository.save(EntityInitializer.getNetworkEntity(parameterNetworkId, new LocalisedString("Verkosto 1", null, null), Arrays.asList(orgTuni, orgUef),
                DtoInitializer.getFixedValidity(OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(1)), false));

        networkRepository.save(EntityInitializer.getNetworkEntity(wrongNetworkId, new LocalisedString("Verkosto 2", null, null), Arrays.asList(orgTuni, orgUef),
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true));

        RealisationSearchParameters searchParameters = new RealisationSearchParameters();
        searchParameters.setIncludeInactive(false);
        searchParameters.setIncludeOwn(false);
        searchParameters.setLanguage(Language.FI);
        searchParameters.setNetworkIdentifiers(Collections.singletonList(wrongNetworkId));
        searchParameters.setPage(0);
        searchParameters.setPageSize(25);
        searchParameters.setQuery("Metro");

        RealisationSearchResults searchResults = this.realisationService.search(organisationRunningQuery, searchParameters);

        assertEquals(0, searchResults.getResults().size());
    }

    @Test
    public void testSearchIncludePastFiltering_shouldOnlyFindRealisationsWithGivenFilter() {
        String organisationRunningQuery = "TUNI";

        RealisationSearchParameters searchParametersToNotIncludePast = new RealisationSearchParameters();
        searchParametersToNotIncludePast.setOrganizingOrganisationId("METROP");
        searchParametersToNotIncludePast.setIncludePast(false);
        RealisationSearchResults notIncludePastResult = this.realisationService.search(organisationRunningQuery, searchParametersToNotIncludePast);

        List<RealisationEntity> expectedResults = validExistingRealisations.stream()
                .filter(r -> r.getOrganizingOrganisationId().equals("METROP"))
                .collect(Collectors.toList());

        assertEquals(expectedResults.size(), notIncludePastResult.getResults().size());
        assertTrue(notIncludePastResult.getResults().stream()
                .allMatch(r -> validInPastRealisations.stream()
                        .noneMatch(r2 -> r2.getRealisationId().equals(r.getRealisationId())
                                && r2.getRealisationIdentifierCode().equals(r.getRealisationIdentifierCode()))));

        RealisationSearchParameters searchParameters = new RealisationSearchParameters();
        searchParameters.setOrganizingOrganisationId("METROP");
        searchParameters.setIncludePast(true);
        RealisationSearchResults includePastResult = this.realisationService.search(organisationRunningQuery, searchParameters);

        expectedResults = validExistingRealisations.stream()
                .filter(r -> r.getOrganizingOrganisationId().equals("METROP"))
                .collect(Collectors.toList());

        expectedResults.addAll(validInPastRealisations);

        assertEquals(expectedResults.size(), includePastResult.getResults().size());
        assertTrue(includePastResult.getResults().stream()
                .anyMatch(r -> validInPastRealisations.stream()
                        .anyMatch(r2 -> r2.getRealisationId().equals(r.getRealisationId())
                                && r2.getRealisationIdentifierCode().equals(r.getRealisationIdentifierCode()))));
    }

    @Test
    public void testDenormalisation_removeDenormalizedRealisationsFromCourseUnit_shouldSucceed() throws JsonProcessingException {

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU1", "UEF",
                null, new LocalisedString("test", null, null));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference cuReference = new StudyElementReference(courseUnitEntity.getStudyElementId(),
                courseUnitEntity.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("UEF", "uef-queue",
                new LocalisedString("uef", null, null), 8);
        organisationRepository.create(organisationEntity);

        Organisation organisation = DtoInitializer.getOrganisation("UEF", "UEF");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("REAL1", "UEF",
                Collections.singletonList(cuReference), null);

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("REAL2", "UEF",
                Collections.singletonList(cuReference), null);

        realisation.setOrganisationReferences(Collections.singletonList(organisationReference));
        realisation = (RealisationEntity) realisationService.create(realisation).get(0).getCurrent();

        realisation2.setOrganisationReferences(Collections.singletonList(organisationReference));
        realisation2 = (RealisationEntity) realisationService.create(realisation2).get(0).getCurrent();

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(2, courseUnitEntity.getRealisations().size());
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
                hasProperty("realisationId", is(realisation.getRealisationId())),
                hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        String updateMessage =
                "{\n" +
                        "    \"realisation\": {\n" +
                        "        \"realisationId\": \"REAL1\",\n" +
                        "        \"studyElementReferences\": []\n" +
                        "   }\n" +
                        "}";

        JsonNode updateJsonNode = objectMapper.readTree(updateMessage);

        realisation = (RealisationEntity) realisationService.update(updateJsonNode.get("realisation"), "UEF").get(0).getCurrent();

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(1, courseUnitEntity.getRealisations().size());
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
                hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));
    }

    private RealisationEntity generateRealisation(String networkId, LocalDate networkStartDate, LocalDate networkEndDate,
                                                  String id, String code, String organizingOrganisationId,
                                                  String nameFi, String nameEn, String nameSv,
                                                  List<StudyElementReference> references,
                                                  OffsetDateTime enrollmentStart, OffsetDateTime enrollmentEnd,
                                                  LocalDate startDate, LocalDate endDate) {
        CooperationNetwork network = null;
        if(networkId != null) {
            network = DtoInitializer.getCooperationNetwork(networkId, new LocalisedString("Verkosto", null, null),
                    true, networkStartDate, networkEndDate);
        }

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                id, code, organizingOrganisationId, references, Collections.singletonList(network));
        realisationEntity.setName(new LocalisedString(nameFi, nameEn, nameSv));

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = new OrganisationReference();
        organisationReference.setOrganisationRole(OrganisationRole.ROLE_MAIN_ORGANIZER);
        organisationReference.setOrganisation(organisation);

        realisationEntity.setOrganisationReferences(Collections.singletonList(organisationReference));

        realisationEntity.setStatus(StudyStatus.ACTIVE);
        realisationEntity.setEnrollmentStartDateTime(enrollmentStart);
        realisationEntity.setEnrollmentEndDateTime(enrollmentEnd);
        realisationEntity.setStartDate(startDate);
        realisationEntity.setEndDate(endDate);
        return realisationRepository.create(realisationEntity);
    }

}
