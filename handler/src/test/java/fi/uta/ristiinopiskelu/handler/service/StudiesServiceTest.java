package fi.uta.ristiinopiskelu.handler.service;


import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class StudiesServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(StudiesServiceTest.class);

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private StudiesService studiesService;

    @Value("${general.messageSchema.version}")
    private int messageSchemaVersion;

    private List<NetworkEntity> existingNetworks;
    private List<NetworkEntity> validNetworks;
    private List<NetworkEntity> invalidValidityNetworks;
    private List<StudyElementEntity> allExistingStudyElements;
    private List<CourseUnitEntity> existingCourseUnits;
    private List<StudyModuleEntity> existingStudyModules;
    private List<StudyElementEntity> studyElementsWithSimilarNames;

    @BeforeEach
    public void beforeAll() {
        List<NetworkOrganisation> orgs = new ArrayList<>();
        List<String> orgIds = Arrays.asList("TUNI", "UEF", "HAAGAH", "METROP", "JYU");
        for(String orgId : orgIds) {
            orgs.add(new NetworkOrganisation(orgId, false, new Validity(Validity.ContinuityEnum.INDEFINITELY,
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

        studyElementsWithSimilarNames = new ArrayList<>();

        createCourseUnitData();
        createStudyModulesData();

        allExistingStudyElements = new ArrayList<>();
        allExistingStudyElements.addAll(existingCourseUnits);
        allExistingStudyElements.addAll(existingStudyModules);
    }

    private void createStudyModulesData() {
        existingStudyModules = new ArrayList<>();

        existingStudyModules.add(this.generateStudyModule("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "SM-1", "SMCODE-1", "TUNI","testi kokonaisuus 1", "test study module 1", "test module 1",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingStudyModules.add(this.generateStudyModule("CN1", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "SM-2", "SMCODE-2", "TUNI","testi kokonaisuus 2", "test study module 2", "test module 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        StudyModuleEntity studyModuleWithSimilarName = this.generateStudyModule("CN1", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "SM-3", "SMCODE-3", "HAAGAH","testi kokonaisuus JuustO", "test CHEESE module 2", "test öSt 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        existingStudyModules.add(studyModuleWithSimilarName);
        studyElementsWithSimilarNames.add(studyModuleWithSimilarName);
    }

    private void createCourseUnitData() {
        existingCourseUnits = new ArrayList<>();

        existingCourseUnits.add(this.generateCourseUnit("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU1", "CU1-ID1", "TUNI","testikurssi 1", "test course 1", "test kurs 1",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN2", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU1-2", "CU1-2-ID1", "TUNI","testikurssi 2", "test course 2", "test kurs 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU2", "CU2-ID1", "UEF","höpökurssi 2", "höpö course 2", "höpö kurs 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU9", "CU9-ID1", "UEF","höpökurssi 2", "höpö course 2", "höpö kurs 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU3", "CU3-ID1", "HAAGAH","älämölöä apinoille 3", "noise for monkeyse 3", "noice för monkiis 3",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU4", "CU4-ID1", "METROP","kaljanjuonnin persuteet 4", "beer drinking basics 4", "öl drickar basicerna 4",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        existingCourseUnits.add(this.generateCourseUnit("CN1", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU5", "CU5-ID1", "JYU","Makkarantekoa, pikakurssi", "Sausage making, instaclass", "Göra sausagerna, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)));

        CourseUnitEntity similarNameCourseUnit = this.generateCourseUnit("CN1", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU6", "CU6-ID1", "JYU","Juustontekoa, pikakurssi", "Cheese making, instaclass", "Göraöst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        existingCourseUnits.add(similarNameCourseUnit);
        studyElementsWithSimilarNames.add(similarNameCourseUnit);

        CourseUnitEntity similarNameCourseUnit2 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU7", "CU7-ID1", "HAAGAH","uusi JUUSTOpikakurssi", "Cheez making, instaCHEESEclass", "snabbtr kurs, Öst",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        existingCourseUnits.add(similarNameCourseUnit2);
        studyElementsWithSimilarNames.add(similarNameCourseUnit2);

        CourseUnitEntity similarNameCourseUnit3 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU8", "CU8-ID1", "UEF","uusi juusto", "Cheez making, instaclass cheese", "ÖST, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        existingCourseUnits.add(similarNameCourseUnit3);
        studyElementsWithSimilarNames.add(similarNameCourseUnit3);
    }

    @Test
    public void testSearchOrganizingOrganisationFiltering_shouldReturnOnlyOtherOrganizerStudies() {
        String organisationRunningQuery = "TUNI";
        StudiesSearchParameters params = new StudiesSearchParameters();
        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, params);

        int noOwnOrganisationSize = allExistingStudyElements.stream()
                .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
                .collect(Collectors.toList())
                .size();

        assertEquals(noOwnOrganisationSize, results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(se -> se.getOrganisationReferences().stream()
                    .anyMatch(or -> or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER
                        && or.getOrganisation().getOrganisationTkCode().equals(organisationRunningQuery))));
    }


    @Test
    public void testSearchOrganizingOrganisationFiltering_shouldReturnIncludeOwnOrganizerStudies() {
        String organisationRunningQuery = "TUNI";
        StudiesSearchParameters params = new StudiesSearchParameters();
        params.setIncludeOwn(true);
        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, params);

        assertEquals(allExistingStudyElements.size(), results.getTotalHits());
    }


    @Test
    public void testSearchStudyElementNetworkValidityFiltering_shouldExcludeAllNonValidNetworks() {
        String organisationRunningQuery = "TUNI";

        CourseUnitEntity invalidCourseUnitEntity1 = this.generateCourseUnit(invalidValidityNetworks.get(0).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU99", "CU99-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity invalidCourseUnitEntity2 = this.generateCourseUnit(invalidValidityNetworks.get(1).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU100", "CU100-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity invalidCourseUnitEntity3 = this.generateCourseUnit(invalidValidityNetworks.get(2).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU101", "CU101-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        StudyModuleEntity invalidValidityStudyModule = this.generateStudyModule(invalidValidityNetworks.get(3).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "SM-123", "SMCODE-123", "JYU","testi kokonaisuus 2", "test study module 2", "test module 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        StudyModuleEntity invalidValidityStudyModule2 = this.generateStudyModule(invalidValidityNetworks.get(1).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "SM-124", "SMCODE-124", "JYU","testi kokonaisuus 2", "test study module 2", "test module 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeOwn(true);
        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, searchParameters);

        assertEquals(allExistingStudyElements.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidCourseUnitEntity1.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidCourseUnitEntity1.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidCourseUnitEntity2.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidCourseUnitEntity2.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidCourseUnitEntity3.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidCourseUnitEntity3.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidValidityStudyModule.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidValidityStudyModule.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidValidityStudyModule2.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidValidityStudyModule2.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getCooperationNetworks().stream()
                .anyMatch(cn -> invalidValidityNetworks.stream().anyMatch(in -> in.getId().equals(cn.getId())))));
    }

    @Test
    public void testSearchOrganisationValidationInNetworkFiltering_shouldReturnOnlyOtherOrganizerStudiesAndExcludeInActiveNetworks() {
        String organisationRunningQuery = "TUNI";

        Validity futureValidity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2));
        Validity pastValidity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().minusYears(2), OffsetDateTime.now().minusYears(1));
        Validity futureValidity2 = new Validity(Validity.ContinuityEnum.INDEFINITELY, OffsetDateTime.now().plusYears(1), null);

        NetworkEntity networkEntity = validNetworks.get(0);
        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(futureValidity);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeOwn(true);

        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, searchParameters);

        List<StudyElementEntity> expectedResults = allExistingStudyElements.stream()
                .filter(se -> se.getCooperationNetworks().stream().noneMatch(cn -> cn.getId().equals(networkEntity.getId())))
                .collect(Collectors.toList());

        assertEquals(expectedResults.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));

        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(pastValidity);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        results = this.studiesService.search(organisationRunningQuery, searchParameters);
        assertEquals(expectedResults.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));

        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(futureValidity2);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        results = this.studiesService.search(organisationRunningQuery, searchParameters);
        assertEquals(expectedResults.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(se -> se.getCooperationNetworks().stream().anyMatch(cn -> cn.getId().equals(networkEntity.getId()))));
    }

    @Test
    public void testSearchStudyElementCooperationNetworkValidityFiltering_shouldReturnOnlyOtherOrganizerStudiesAndExcludeInActive() {
        String organisationRunningQuery = "TUNI";

        List<StudyElementEntity> studyElementsThatShouldReturn = allExistingStudyElements.stream()
                .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
                .collect(Collectors.toList());

        // Cooperation network validity in past
        CourseUnitEntity invalidCourseUnitEntity1 = this.generateCourseUnit("CN1", LocalDate.now().minusYears(2), LocalDate.now().minusYears(1),
                "CU99", "CU99-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Cooperation network validity in future
        CourseUnitEntity invalidCourseUnitEntity2 = this.generateCourseUnit("CN1", LocalDate.now().plusYears(2), LocalDate.now().plusYears(3),
                "CU100", "CU100-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Cooperation network validity null (so this should be found)
        CourseUnitEntity correctValidityCourseUnit = this.generateCourseUnit("CN2", null, null,
                "CU101", "CU101-ID1", "JYU","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        studyElementsThatShouldReturn.add(correctValidityCourseUnit);

        // Study module that has cooperation network validity in past
        StudyModuleEntity invalidValidityStudyModule = this.generateStudyModule("CN2", LocalDate.now().minusYears(2), LocalDate.now().minusYears(1),
                "SM-123", "SMCODE-123", "JYU","testi kokonaisuus 2", "test study module 2", "test module 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Study module that has cooperation network validity start but end null (should be found)
        StudyModuleEntity correctValidityStudyModule = this.generateStudyModule("CN1", LocalDate.now().minusYears(2), null,
                "SM-124", "SMCODE-124", "JYU","testi kokonaisuus 4", "test study module 4", "test module 4",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        studyElementsThatShouldReturn.add(correctValidityStudyModule);

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, searchParameters);

        assertEquals(studyElementsThatShouldReturn.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidCourseUnitEntity1.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidCourseUnitEntity1.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidCourseUnitEntity2.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidCourseUnitEntity2.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().noneMatch(r -> r.getStudyElementId().equals(invalidValidityStudyModule.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(invalidValidityStudyModule.getStudyElementIdentifierCode())));
        assertTrue(results.getResults().stream().anyMatch(r -> r.getStudyElementId().equals(correctValidityCourseUnit.getStudyElementId())
                && r.getStudyElementIdentifierCode().equals(correctValidityCourseUnit.getStudyElementIdentifierCode())));
    }

    @Test
    public void testSearchNameFiltering_shouldBeCaseInsensitive() {
        String organisationRunningQuery = "TUNI";

        List<StudyElementEntity> studyElementsThatShouldReturn = studyElementsWithSimilarNames.stream()
                .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
                .collect(Collectors.toList());

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setQuery("Juusto");
        
        StudiesSearchResults results = this.studiesService.search(organisationRunningQuery, searchParameters);
        assertEquals(studyElementsThatShouldReturn.size(), results.getTotalHits());
        assertTrue(results.getResults().stream().allMatch(
                se -> se.getName().getValue("fi").toLowerCase().contains(searchParameters.getQuery().toLowerCase())));

        StudiesSearchParameters enSearchParameters = new StudiesSearchParameters();
        enSearchParameters.setQuery("cheese");
        enSearchParameters.setLanguage(Language.EN);

        StudiesSearchResults engSearchResults = this.studiesService.search(organisationRunningQuery, enSearchParameters);
        assertEquals(studyElementsThatShouldReturn.size(), engSearchResults.getTotalHits());
        assertTrue(engSearchResults.getResults().stream().allMatch(
                se -> se.getName().getValue("en").toLowerCase().contains(enSearchParameters.getQuery().toLowerCase())));

        StudiesSearchParameters svSearchParameters = new StudiesSearchParameters();
        svSearchParameters.setQuery("ÖST");
        svSearchParameters.setLanguage(Language.SV);

        StudiesSearchResults svSearchResults = this.studiesService.search(organisationRunningQuery, svSearchParameters);
        assertEquals(studyElementsThatShouldReturn.size(), svSearchResults.getTotalHits());
        assertTrue(svSearchResults.getResults().stream().allMatch(
                se -> se.getName().getValue("sv").toLowerCase().contains(svSearchParameters.getQuery().toLowerCase())));
    }


    @Test
    public void testOrganisationFiltering_shouldReturnStudyElementsWithOnlyGivenOrganisations() {
        String organisationRunningQuery = "TUNI";

        // Test with includeOwn = false
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setOrganizingOrganisationIdentifiers(Arrays.asList("JYU", "HAAGAH"));

        List<StudyElementEntity> studyElementsThatShouldReturn = allExistingStudyElements.stream()
                .filter(se -> searchParameters.getOrganizingOrganisationIdentifiers().contains(se.getOrganizingOrganisationId()))
                .collect(Collectors.toList());

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);
        assertEquals(studyElementsThatShouldReturn.size(), searchResults.getTotalHits());
        assertTrue(searchResults.getResults().stream()
                .allMatch(se -> se.getOrganisationReferences().stream()
                        .anyMatch(or -> searchParameters.getOrganizingOrganisationIdentifiers().contains(or.getOrganisation().getOrganisationTkCode())
                                && or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER)));

        // Test with includeOwn = true. we shouldn't get any own studies since organizingOrganisationIdentifiers is defined
        StudiesSearchParameters includeOwnSearchParameters = new StudiesSearchParameters();
        includeOwnSearchParameters.setOrganizingOrganisationIdentifiers(Arrays.asList("JYU", "HAAGAH"));
        includeOwnSearchParameters.setIncludeOwn(true);

        StudiesSearchResults includeOwnSearchResults = this.studiesService.search(organisationRunningQuery, includeOwnSearchParameters);
        assertEquals(studyElementsThatShouldReturn.size(), includeOwnSearchResults.getTotalHits());
        assertTrue(includeOwnSearchResults.getResults().stream().allMatch(se ->
                se.getOrganisationReferences().stream().anyMatch(or ->
                        (searchParameters.getOrganizingOrganisationIdentifiers().contains(or.getOrganisation().getOrganisationTkCode())
                                || or.getOrganisation().getOrganisationTkCode().equals(organisationRunningQuery))
                        && or.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER)));
    }

    @Test
    public void testPaging_shouldExcludeOwnOrganisationCourseUnitsPaged() {
        String organisationRunningQuery = "TUNI";

        List<StudyElementEntity> studyElementsThatShouldReturn = existingCourseUnits.stream()
                .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
                .collect(Collectors.toList());

        List<AbstractStudyElementReadDTO> foundStudyElements = new ArrayList<>();

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setType(StudiesSearchElementType.COURSE_UNIT);
        searchParameters.setPage(0);
        searchParameters.setPageSize(3);
        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);
        foundStudyElements.addAll(searchResults.getResults());

        searchParameters.setPage(1);
        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);
        foundStudyElements.addAll(searchResults.getResults());

        searchParameters.setPage(2);
        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);
        foundStudyElements.addAll(searchResults.getResults());

        searchParameters.setPage(3);
        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);
        foundStudyElements.addAll(searchResults.getResults());

        assertEquals(studyElementsThatShouldReturn.size(), foundStudyElements.size());
        for(StudyElementEntity existingStudyElement : studyElementsThatShouldReturn) {
            assertTrue(foundStudyElements.stream().anyMatch(se -> se.getStudyElementId().equals(existingStudyElement.getStudyElementId())
                    && se.getStudyElementIdentifierCode().equals(existingStudyElement.getStudyElementIdentifierCode())));
        }
    }

    @Test
    public void testTypeFiltering_shouldReturnTypedResults() {
        String organisationRunningQuery = "TUNI";

        List<StudyElementEntity> courseUnitsThatShouldReturn = existingCourseUnits.stream()
                .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
                .collect(Collectors.toList());

        StudiesSearchParameters cuSearchParameters = new StudiesSearchParameters();
        cuSearchParameters.setType(StudiesSearchElementType.COURSE_UNIT);

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, cuSearchParameters);

        assertEquals(courseUnitsThatShouldReturn.size(), searchResults.getTotalHits());

        List<StudyElementEntity> studyModulesThatShouldReturn = existingStudyModules.stream()
            .filter(cu -> !cu.getOrganizingOrganisationId().equals(organisationRunningQuery))
            .collect(Collectors.toList());

        StudiesSearchParameters allSearchParameters = new StudiesSearchParameters();
        allSearchParameters.setType(StudiesSearchElementType.ALL);

        StudiesSearchResults allSearchResults = this.studiesService.search(organisationRunningQuery, allSearchParameters);

        assertEquals(courseUnitsThatShouldReturn.size() + studyModulesThatShouldReturn.size(), allSearchResults.getTotalHits());

        // none of the courseUnits in courseUnitsThatShouldReturn have any realisations, therefore no CUs returned
        allSearchParameters.setType(StudiesSearchElementType.ALL);
        allSearchParameters.setRealisationStatuses(Collections.singletonList(StudyStatus.ACTIVE));

        allSearchResults = this.studiesService.search(organisationRunningQuery, allSearchParameters);

        assertEquals(studyModulesThatShouldReturn.size(), allSearchResults.getTotalHits());
        
        StudiesSearchParameters smSearchParameters = new StudiesSearchParameters();
        smSearchParameters.setType(StudiesSearchElementType.STUDY_MODULE);
        StudiesSearchResults smSearchResults = this.studiesService.search(organisationRunningQuery, smSearchParameters);

        assertEquals(studyModulesThatShouldReturn.size(), smSearchResults.getTotalHits());
    }

    @Test
    public void testSearch_shouldReturnStudyElementWithNoCooperationNetwork() {
        String organisationRunningQuery = "TUNI";

        CourseUnitEntity courseUnitWithoutNetwork = EntityInitializer.getCourseUnitEntity("CU-101", "CUCODE-101", organisationRunningQuery,
                null, new LocalisedString("Ilman verkostoa", "No network", null));

        Organisation organisation = DtoInitializer.getOrganisation(organisationRunningQuery, organisationRunningQuery);
        OrganisationReference organisationReference = new OrganisationReference();
        organisationReference.setOrganisationRole(OrganisationRole.ROLE_MAIN_ORGANIZER);
        organisationReference.setOrganisation(organisation);
        courseUnitWithoutNetwork.setOrganisationReferences(Collections.singletonList(organisationReference));

        courseUnitWithoutNetwork.setStatus(StudyStatus.ACTIVE);
        courseUnitWithoutNetwork.setValidityStartDate(LocalDate.now().minusYears(1));
        courseUnitWithoutNetwork.setValidityEndDate(LocalDate.now().plusYears(1));

        allExistingStudyElements.add(courseUnitRepository.create(courseUnitWithoutNetwork));

        CourseUnitEntity courseUnitWithoutNetwork2 = EntityInitializer.getCourseUnitEntity("CU-101", "CUCODE-101", organisationRunningQuery,
                Collections.emptyList(), new LocalisedString("Ilman verkostoa", "No network", null));
        courseUnitWithoutNetwork2.setOrganisationReferences(Collections.singletonList(organisationReference));
        courseUnitWithoutNetwork2.setStatus(StudyStatus.ACTIVE);
        courseUnitWithoutNetwork2.setValidityStartDate(LocalDate.now().minusYears(1));
        courseUnitWithoutNetwork2.setValidityEndDate(LocalDate.now().plusYears(1));

        allExistingStudyElements.add(courseUnitRepository.create(courseUnitWithoutNetwork2));

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeInactive(true);
        searchParameters.setIncludeOwn(true);

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        assertEquals(allExistingStudyElements.size(), searchResults.getTotalHits());
        assertTrue(searchResults.getResults().stream().anyMatch(
                se -> se.getStudyElementId().equals(courseUnitWithoutNetwork.getStudyElementId())
                    && se.getStudyElementIdentifierCode().equals(courseUnitWithoutNetwork.getStudyElementIdentifierCode())));
        assertTrue(searchResults.getResults().stream().anyMatch(
                se -> se.getStudyElementId().equals(courseUnitWithoutNetwork2.getStudyElementId())
                        && se.getStudyElementIdentifierCode().equals(courseUnitWithoutNetwork2.getStudyElementIdentifierCode())));
        assertTrue(searchResults.getResults().stream().anyMatch(se -> se.getCooperationNetworks() == null));
    }

    @Test
    public void testAllFilters_shouldReturnValidUefCourseUnit() {
        String organisationRunningQuery = "TUNI";

        // Setup TUNI's validity in network CN-1 into future
        Validity futureValidity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2));

        NetworkEntity networkEntity = validNetworks.get(0);
        networkEntity.getOrganisations().removeIf(no -> no.getOrganisationTkCode().equals(organisationRunningQuery));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organisationRunningQuery);
        networkOrganisation.setValidityInNetwork(futureValidity);
        networkEntity.getOrganisations().add(networkOrganisation);

        networkRepository.update(networkEntity);

        // Generate invalid course unit that has invalid vality in network
        CourseUnitEntity invalidCourseUnitEntity1 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(2), LocalDate.now().minusYears(1),
                "CU99", "CU99-ID1", "UEF","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Generate course unit that has invalid vality in network
        CourseUnitEntity invalidCourseUnitEntity2 = this.generateCourseUnit(invalidValidityNetworks.get(0).getId(), LocalDate.now().minusYears(2), LocalDate.now().plusYears(1),
                "CU101", "CU101-ID1", "UEF","Juustontekoa, pikakurssi", "Cheez making, instaclass", "Göra öst, snabbtr kurs",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        // Generate course unit that is otherwise valid but name does not match
        this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                "CU102", "CU102-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setOrganizingOrganisationIdentifiers(Arrays.asList("UEF"));
        searchParameters.setType(StudiesSearchElementType.COURSE_UNIT);
        searchParameters.setQuery("höpö");

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        assertEquals(1, searchResults.getTotalHits());
        assertEquals("CU9", searchResults.getResults().get(0).getStudyElementId());
        assertEquals("CU9-ID1", searchResults.getResults().get(0).getStudyElementIdentifierCode());
        assertEquals("UEF", searchResults.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());
    }

    @Test
    public void testSearchStudyElementCooperationNetworkValidityFiltering_organisationDoesNotBelongToAnyValidNetwork_ShouldNotReturnAnyResults() {
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

        this.generateStudyModule(wrongNetworkId, LocalDate.of(2019, 10, 18), LocalDate.of(2019, 11, 4), "YVQRjW4B5SA-v7gmeav7", "A123", "UEF",
                "Testi Metro Erillinen A123 (qa1) 21.11.2019",  "Testi Metro Erillinen A123 (qa1) 21.11.2019", null, LocalDate.of(2018, 8, 1), LocalDate.of(2022, 7, 31));

        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeInactive(false);
        searchParameters.setIncludeOwn(false);
        searchParameters.setLanguage(Language.FI);
        searchParameters.setNetworkIdentifiers(Arrays.asList(wrongNetworkId));
        searchParameters.setPage(0);
        searchParameters.setPageSize(25);
        searchParameters.setQuery("Metro");

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        assertEquals(0, searchResults.getTotalHits());
    }

    @Test
    public void testSearch_filterByRealisationDate_ShouldReturnStudyModulesAndCourseUnits() {
        String organisationRunningQuery = "TUNI";

        // Generate course unit with realisation
        CourseUnitEntity courseUnitWithRealisation = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU102", "CU102-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity denormalizedData = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("Toteutus 1", null, null), LocalDate.now().minusMonths(1), null, null, null);

        courseUnitWithRealisation.setRealisations(Collections.singletonList(denormalizedData));
        courseUnitRepository.update(courseUnitWithRealisation);

        // Generate course unit with assessment item realisation
        CourseUnitEntity courseUnitWithAiRealisation = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU103", "CU103-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-2", "CU103");
        AssessmentItemEntity assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-1", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(denormalizedData));

        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntityWithRealisation));
        CompletionOptionEntity dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisation.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisation);


        // Generate course unit with realisations that are available in future
        CourseUnitEntity courseUnitWithRealisationInFuture = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU104", "CU104-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity futureRealisation = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("Toteutus 1", null, null), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3), null, null);

        courseUnitWithRealisationInFuture.setRealisations(Collections.singletonList(futureRealisation));
        courseUnitRepository.update(courseUnitWithRealisationInFuture);

        CourseUnitEntity courseUnitWithAiRealisationInFuture = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU105", "CU105-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-3", "CU103");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-4", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(futureRealisation));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-3", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-4", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationInFuture.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationInFuture);

        // Search by realisation start date now
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationStartDate(LocalDate.now());
        searchParameters.setIncludeOwn(true);

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (should exclude course units with realisation in future)
        assertEquals(existingStudyModules.size() + 2, searchResults.getTotalHits());

        List<AbstractStudyElementReadDTO> courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(2, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));


        // Generate course unit with realisation with end time (still currently valid)
        CourseUnitEntity courseUnitWithRealisation2 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU106", "CU106-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity denormalizedDataWithEndTime = EntityInitializer.getCourseUnitRealisation("R3", "RCODE3", organisationRunningQuery,
            new LocalisedString("Toteutus 3", null, null), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1), null, null);

        courseUnitWithRealisation2.setRealisations(Collections.singletonList(denormalizedDataWithEndTime));
        courseUnitRepository.update(courseUnitWithRealisation2);

        // Generate course unit with assessment item realisation with end time (still currently valid)
        CourseUnitEntity courseUnitWithAiRealisation2 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU107", "CU107-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-5", "CU107");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-6", "CU107");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(denormalizedDataWithEndTime));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-5", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-6", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisation2.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisation2);


        // Generate course unit with realisations in past
        CourseUnitEntity courseUnitWithRealisationInPast = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU108", "CU108-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationInPast = EntityInitializer.getCourseUnitRealisation("R4", "RCODE4", organisationRunningQuery,
            new LocalisedString("Toteutus 3", null, null), LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), null, null);

        courseUnitWithRealisationInPast.setRealisations(Collections.singletonList(realisationInPast));
        courseUnitRepository.update(courseUnitWithRealisationInPast);

        // Generate course unit with assessment item realisations in past
        CourseUnitEntity courseUnitWithAiRealisationInPast = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU109", "CU109-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-7", "CU109");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-8", "CU109");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(realisationInPast));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-7", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-8", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationInPast.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationInPast);

        // Search by realisation start date now
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationEndDate(LocalDate.now());
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (exclude past realisations)
        assertEquals(existingStudyModules.size() + 6, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(6, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationInFuture.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationInFuture.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation2.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation2.getStudyElementId())));


        // Search by realisation start date and end date
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationStartDate(LocalDate.now().minusDays(1));
        searchParameters.setRealisationEndDate(LocalDate.now().plusDays(1));
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (exclude past and future realisations)
        assertEquals(existingStudyModules.size() + 4, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(4, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation2.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation2.getStudyElementId())));
    }

    @Test
    public void testSearch_filterByRealisationEnrollmentDateTime_ShouldReturnStudyModulesAndCourseUnits() {
        String organisationRunningQuery = "TUNI";

        // Generate course unit with realisation
        CourseUnitEntity courseUnitWithRealisation = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU102", "CU102-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity denormalizedData = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("Toteutus 1", null, null), null, null, OffsetDateTime.now().minusMonths(1), null);


        courseUnitWithRealisation.setRealisations(Collections.singletonList(denormalizedData));
        courseUnitRepository.update(courseUnitWithRealisation);

        // Generate course unit with assessment item realisation
        CourseUnitEntity courseUnitWithAiRealisation = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU103", "CU103-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AssessmentItemEntity dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-2", "CU103");
        AssessmentItemEntity assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-1", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(denormalizedData));

        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntityWithRealisation));
        CompletionOptionEntity dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisation.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisation);


        // Generate course unit with realisations that are available in future
        CourseUnitEntity courseUnitWithRealisationInFuture = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU104", "CU104-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity futureRealisation = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("Toteutus 1", null, null),null, null,  OffsetDateTime.now().plusMonths(1), OffsetDateTime.now().plusMonths(3));

        courseUnitWithRealisationInFuture.setRealisations(Collections.singletonList(futureRealisation));
        courseUnitRepository.update(courseUnitWithRealisationInFuture);

        CourseUnitEntity courseUnitWithAiRealisationInFuture = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU105", "CU105-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-3", "CU103");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-4", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(futureRealisation));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-3", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-4", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationInFuture.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationInFuture);

        // Search by realisation start date now
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationEnrollmentStartDateTime(OffsetDateTime.now());
        searchParameters.setIncludeOwn(true);

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (should exclude course units with realisation in future)
        assertEquals(existingStudyModules.size() + 2, searchResults.getTotalHits());

        List<AbstractStudyElementReadDTO> courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(2, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));


        // Generate course unit with realisation with end time (still currently valid)
        CourseUnitEntity courseUnitWithRealisation2 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU106", "CU106-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity denormalizedDataWithEndTime = EntityInitializer.getCourseUnitRealisation("R3", "RCODE3", organisationRunningQuery,
            new LocalisedString("Toteutus 3", null, null),null, null,  OffsetDateTime.now().minusMonths(1), OffsetDateTime.now().plusMonths(1));

        courseUnitWithRealisation2.setRealisations(Collections.singletonList(denormalizedDataWithEndTime));
        courseUnitRepository.update(courseUnitWithRealisation2);

        // Generate course unit with assessment item realisation with end time (still currently valid)
        CourseUnitEntity courseUnitWithAiRealisation2 = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU107", "CU107-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-5", "CU107");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-6", "CU107");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(denormalizedDataWithEndTime));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-5", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-6", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisation2.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisation2);


        // Generate course unit with realisations in past
        CourseUnitEntity courseUnitWithRealisationInPast = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU108", "CU108-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationInPast = EntityInitializer.getCourseUnitRealisation("R4", "RCODE4", organisationRunningQuery,
            new LocalisedString("Toteutus 3", null, null), null, null, OffsetDateTime.now().minusMonths(2), OffsetDateTime.now().minusMonths(1));

        courseUnitWithRealisationInPast.setRealisations(Collections.singletonList(realisationInPast));
        courseUnitRepository.update(courseUnitWithRealisationInPast);

        // Generate course unit with assessment item realisations in past
        CourseUnitEntity courseUnitWithAiRealisationInPast = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU109", "CU109-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-7", "CU109");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-8", "CU109");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(realisationInPast));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-7", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-8", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationInPast.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationInPast);

        // Search by realisation start date now
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationEnrollmentEndDateTime(OffsetDateTime.now());
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (exclude past realisations)
        assertEquals(existingStudyModules.size() + 6, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(6, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationInFuture.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationInFuture.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation2.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation2.getStudyElementId())));


        // Search by realisation start date and end date
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationEnrollmentStartDateTime(OffsetDateTime.now().minusDays(1));
        searchParameters.setRealisationEnrollmentEndDateTime(OffsetDateTime.now().plusDays(1));
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit created in this test (exclude past and future realisations)
        assertEquals(existingStudyModules.size() + 4, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(4, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisation2.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisation2.getStudyElementId())));
    }

    @Test
    public void testSearch_filterByRealisationQuery_ShouldReturnStudyModulesAndCourseUnits() {
        String organisationRunningQuery = "TUNI";

        // Generate course unit with realisation
        CourseUnitEntity courseUnitWithRealisationCoffeeName = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU101", "CU101-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationCoffeeName = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("KAHVIA ja pullaa", "coffee REAL", "asd kafFE "), null, null, OffsetDateTime.now().minusMonths(1), null);

        courseUnitWithRealisationCoffeeName.setRealisations(Collections.singletonList(realisationCoffeeName));
        courseUnitRepository.update(courseUnitWithRealisationCoffeeName);

        CourseUnitEntity courseUnitWithRealisationCoffeeCode = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU102", "CU102-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationCoffeeCode = EntityInitializer.getCourseUnitRealisation("R2", "COFFEEEEEEEE", organisationRunningQuery,
            new LocalisedString("CH", null, null),null, null,  OffsetDateTime.now().plusMonths(1), OffsetDateTime.now().plusMonths(3));

        courseUnitWithRealisationCoffeeCode.setRealisations(Collections.singletonList(realisationCoffeeCode));
        courseUnitRepository.update(courseUnitWithRealisationCoffeeCode);

        // Generate course unit with assessment item realisation with name as coffee or code
        CourseUnitEntity courseUnitWithAiRealisationCoffee = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU103", "CU103-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationCoffeeName2 = EntityInitializer.getCourseUnitRealisation("R3", "CODE", organisationRunningQuery,
            new LocalisedString("gg KAHVIA ja pullaa cc", "coffee REAL fast", "asd kafFE"), null, null, OffsetDateTime.now().minusMonths(1), null);

        AssessmentItemEntity dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-2", "CU103");
        AssessmentItemEntity assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-1", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(realisationCoffeeName2));

        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntityWithRealisation));
        CompletionOptionEntity dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationCoffee.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationCoffee);

        CourseUnitEntity courseUnitWithAiRealisationCoffeeCode = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU104", "CU104-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisation2 = EntityInitializer.getCourseUnitRealisation("R4", "AAAAAAAAAAACOFFEE", organisationRunningQuery,
            new LocalisedString("Toteutus 1", null, null),null, null,  OffsetDateTime.now().plusMonths(1), OffsetDateTime.now().plusMonths(3));

        dummyAssessmentItem = EntityInitializer.getAssessmentItemEntity("AI-3", "CU103");
        assessmentItemEntityWithRealisation = EntityInitializer.getAssessmentItemEntity("AI-4", "CU103");
        assessmentItemEntityWithRealisation.setRealisations(Collections.singletonList(realisation2));

        completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-3", Collections.singletonList(assessmentItemEntityWithRealisation));
        dummyCompletionOption = EntityInitializer.getCompletionOptionEntity("CO-4", Collections.singletonList(dummyAssessmentItem));
        courseUnitWithAiRealisationCoffeeCode.setCompletionOptions(Arrays.asList(completionOptionEntity, dummyCompletionOption));
        courseUnitRepository.update(courseUnitWithAiRealisationCoffeeCode);

        // Create course unit with realisation that does not include coffee
        CourseUnitEntity courseUnitWithRealisationOther = this.generateCourseUnit("CN2", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
            "CU105", "CU105-ID1", "UEF","KURSSINIMI", "höpö course 2", "höpö kurs 2",
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitRealisationEntity realisationOtherName = EntityInitializer.getCourseUnitRealisation("R1", "RCODE1", organisationRunningQuery,
            new LocalisedString("jotain muuta", "anti-caffeine", "nothing"), null, null, OffsetDateTime.now().minusMonths(1), null);

        courseUnitWithRealisationOther.setRealisations(Collections.singletonList(realisationOtherName));
        courseUnitRepository.update(courseUnitWithRealisationOther);

        // Search by realisation start date now
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationQuery("kahvia ja pullaa");
        searchParameters.setLanguage(Language.FI);
        searchParameters.setIncludeOwn(true);

        StudiesSearchResults searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit with "kahvia ja pullaa" in name
        assertEquals(existingStudyModules.size() + 2, searchResults.getTotalHits());

        List<AbstractStudyElementReadDTO> courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(2, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffee.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeName.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationOther.getStudyElementId())));

        // Search by realisation query
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationQuery("COFFEE");
        searchParameters.setLanguage(Language.EN);
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit with coffee in name or code
        assertEquals(existingStudyModules.size() + 4, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(4, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffee.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeName.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationOther.getStudyElementId())));

        // Search by realisation query lang en
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationQuery("kaffe");
        searchParameters.setLanguage(Language.SV);
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit with "kaffe" in name
        assertEquals(existingStudyModules.size() + 2, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(2, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffee.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeName.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationOther.getStudyElementId())));


        // Search by realisation start date now
        searchParameters = new StudiesSearchParameters();
        searchParameters.setRealisationQuery("jotain muu");
        searchParameters.setLanguage(Language.FI);
        searchParameters.setIncludeOwn(true);

        searchResults = this.studiesService.search(organisationRunningQuery, searchParameters);

        // Should return all study modules and course unit with coffee
        assertEquals(existingStudyModules.size() + 1, searchResults.getTotalHits());

        courseUnitResults = searchResults.getResults().stream().filter(res -> res instanceof CourseUnitReadDTO).collect(Collectors.toList());
        assertEquals(1, courseUnitResults.size());
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffee.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithAiRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeCode.getStudyElementId())));
        assertTrue(courseUnitResults.stream().noneMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationCoffeeName.getStudyElementId())));
        assertTrue(courseUnitResults.stream().anyMatch(cu -> cu.getStudyElementId().equals(courseUnitWithRealisationOther.getStudyElementId())));
    }

    private CourseUnitEntity generateCourseUnit(String networkId, LocalDate networkStartDate, LocalDate networkEndDate,
                                    String id, String code, String organizingOrganisationId,
                                    String nameFi, String nameEn, String nameSv,
                                    LocalDate courseUnitStartDate, LocalDate courseUnitEndDate) {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                networkId, new LocalisedString("Verkosto", null, null), true, networkStartDate, networkEndDate);
        LocalisedString originalName = new LocalisedString(nameFi, nameEn, nameSv);
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
                id, code, organizingOrganisationId, Collections.singletonList(network), originalName);

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = new OrganisationReference();
        organisationReference.setOrganisationRole(OrganisationRole.ROLE_MAIN_ORGANIZER);
        organisationReference.setOrganisation(organisation);
        courseUnit.setOrganisationReferences(Collections.singletonList(organisationReference));

        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnit.setValidityStartDate(courseUnitStartDate);
        courseUnit.setValidityEndDate(courseUnitEndDate);
        return courseUnitRepository.create(courseUnit);
    }

    private StudyModuleEntity generateStudyModule(String networkId, LocalDate networkStartDate, LocalDate networkEndDate,
                                                String id, String code, String organizingOrganisationId,
                                                String nameFi, String nameEn, String nameSv,
                                                LocalDate courseUnitStartDate, LocalDate courseUnitEndDate) {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
                networkId, new LocalisedString("Verkosto", null, null), true, networkStartDate, networkEndDate);
        LocalisedString originalName = new LocalisedString(nameFi, nameEn, nameSv);
        StudyModuleEntity studyModule = EntityInitializer.getStudyModuleEntity(
                id, code, organizingOrganisationId, Collections.singletonList(network), originalName);

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = new OrganisationReference();
        organisationReference.setOrganisationRole(OrganisationRole.ROLE_MAIN_ORGANIZER);
        organisationReference.setOrganisation(organisation);
        studyModule.setOrganisationReferences(Collections.singletonList(organisationReference));

        studyModule.setStatus(StudyStatus.ACTIVE);
        studyModule.setValidityStartDate(courseUnitStartDate);
        studyModule.setValidityEndDate(courseUnitEndDate);
        return studyModuleRepository.create(studyModule);
    }

}
