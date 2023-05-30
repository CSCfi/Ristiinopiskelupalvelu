package fi.uta.ristiinopiskelu.handler.integration.controller.v8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.CourseUnitRestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudiesRestSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies.StudyElementRestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.*;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudiesControllerV8IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Test
    public void testFindStudies_returnsTypeAttribute_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", null, "UEF",
            "Testijakso", coopNetwork, null, null, null);
        StudyModuleEntity studyModuleEntity = createStudyModule("SM1", null, "UEF",
            "Testikokonaisuus", coopNetwork);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setType(StudiesSearchElementType.COURSE_UNIT);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams.setType(StudiesSearchElementType.STUDY_MODULE);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElementType.STUDY_MODULE, actualResult.getResults().get(0).getType());
        assertEquals(studyModuleEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(studyModuleEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());
    }
    @Test
    public void testFindStudies_teachingLanguagesForStudyModules_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitRealisationEntity enrollableRealisationRef = new CourseUnitRealisationEntity();
        enrollableRealisationRef.setEnrollmentStartDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1).getStartDateAsOffset());
        enrollableRealisationRef.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2).getEndDateAsOffset());
        enrollableRealisationRef.setRealisationId("TOT-1");
        enrollableRealisationRef.setOrganizingOrganisationId("UEF");
        enrollableRealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        enrollableRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity notEnrollableRealisationRef = new CourseUnitRealisationEntity();
        notEnrollableRealisationRef.setRealisationId("TOT-2");
        notEnrollableRealisationRef.setOrganizingOrganisationId("UEF");
        notEnrollableRealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        notEnrollableRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", null, "UEF",
            "Testijakso", Collections.singletonList(coopNetwork), Collections.singletonList(enrollableRealisationRef), null, null, Collections.singletonList("sv"));

        CourseUnitEntity courseUnitEntityWithoutTeachingLanguages = createCourseUnit("CU2", null, "UEF",
            "Testijakso", Collections.singletonList(coopNetwork), Collections.singletonList(notEnrollableRealisationRef), null, null, null);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");
        StudyElementReference cu2Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU2", "UEF");

        // the actual realisations
        RealisationEntity realisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF",
            Collections.singletonList(cu2Reference), Collections.singletonList(coopNetwork));

        StudyModuleEntity studyModuleEntity = createStudyModule("SM1", null, "UEF",
            "Testikokonaisuus", coopNetwork);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams.setTeachingLanguages(null);
        searchParams.setRealisationTeachingLanguages(null);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntityWithoutTeachingLanguages.getStudyElementId()))
        ));

        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntityWithoutTeachingLanguages.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        studyModuleEntity.setTeachingLanguage(Collections.singletonList("SV"));
        studyModuleRepository.update(studyModuleEntity);

        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        studyModuleEntity.setStatus(StudyStatus.CANCELLED);
        studyModuleRepository.update(studyModuleEntity);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudies_queryParameterCaseInsensitiveFindsFromNameAndIdentifierCode_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro","UEF",
            "Raipatirai", coopNetwork, null, null, null);
        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka","UEF",
            "Keppihevonen", coopNetwork, null, null, null);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setQuery("esti");

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setQuery("raipati");

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setQuery("UY mat");

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity2.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity2.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());
    }

    @Test
    public void testFindStudies_onlyEnrollable_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");

        CooperationNetwork validCoopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork expiredCoopNetwork = createCooperationNetwork("CN-2");
        expiredCoopNetwork.setValidityEndDate(LocalDate.now().minusDays(1));

        CourseUnitRealisationEntity closedRealisationRef = new CourseUnitRealisationEntity();
        closedRealisationRef.setRealisationId("TOT-1");
        closedRealisationRef.setOrganizingOrganisationId("UEF");
        closedRealisationRef.setCooperationNetworks(Collections.singletonList(validCoopNetwork));
        closedRealisationRef.setEnrollmentClosed(true);
        closedRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity enrollableRealisationRef = new CourseUnitRealisationEntity();
        enrollableRealisationRef.setEnrollmentStartDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1).getStartDateAsOffset());
        enrollableRealisationRef.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2).getEndDateAsOffset());
        enrollableRealisationRef.setRealisationId("TOT-2");
        enrollableRealisationRef.setOrganizingOrganisationId("UEF");
        enrollableRealisationRef.setCooperationNetworks(Collections.singletonList(validCoopNetwork));
        enrollableRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity networkExpiredRealisationRef = new CourseUnitRealisationEntity();
        networkExpiredRealisationRef.setRealisationId("TOT-3");
        networkExpiredRealisationRef.setOrganizingOrganisationId("UEF");
        networkExpiredRealisationRef.setCooperationNetworks(Collections.singletonList(expiredCoopNetwork));
        networkExpiredRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", validCoopNetwork, Collections.singletonList(closedRealisationRef), null, null);

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", validCoopNetwork, Collections.singletonList(enrollableRealisationRef), null, null);

        CompletionOptionEntity completionOption = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem = new AssessmentItemEntity();
        assessmentItem.setRealisations(Collections.singletonList(enrollableRealisationRef));
        completionOption.setAssessmentItems(Collections.singletonList(assessmentItem));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "UEF",
            "Keppivirtahepo", validCoopNetwork, null, null, null);
        courseUnitEntity3.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.update(courseUnitEntity3);

        CourseUnitEntity courseUnitEntity4 = createCourseUnit("CU4", "Testi Hrr", "UEF",
            "Raipatirai", validCoopNetwork, Collections.singletonList(networkExpiredRealisationRef), null, null);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");
        StudyElementReference cu2Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU2", "UEF");
        StudyElementReference cu3Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU3", "UEF");
        StudyElementReference cu4Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU4", "UEF");

        // the actual realisations
        RealisationEntity closedRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(validCoopNetwork));

        RealisationEntity enrollableRealisationEntity = createRealisation("TOT-2", "UEF",
            Arrays.asList(cu2Reference, cu3Reference), Collections.singletonList(validCoopNetwork));

        RealisationEntity expiredNetworkRealisationEntity = createRealisation("TOT-3", "UEF",
            Collections.singletonList(cu4Reference), Collections.singletonList(expiredCoopNetwork));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setOnlyEnrollable(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
                hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
                hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        searchParams.setOnlyEnrollable(false);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudies_filterByTeachingLanguage_caseInsensitive_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "JAR matikka", "UEF",
            "Keppivirtahepo", coopNetwork, null, null, null);
        courseUnitEntity.setTeachingLanguage(Arrays.asList("FI", "sv"));
        courseUnitRepository.update(courseUnitEntity);

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "DOR matikka", "UEF",
            "Keppivirtahepo2", coopNetwork, null, null, null);
        courseUnitEntity2.setTeachingLanguage(Arrays.asList("sv"));
        courseUnitRepository.update(courseUnitEntity2);

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "HÖR matikka", "UEF",
            "Keppivirtahepo3", coopNetwork, null, null, null);
        courseUnitEntity3.setTeachingLanguage(Arrays.asList("EN"));
        courseUnitRepository.update(courseUnitEntity3);

        String query = "{\n" +
            "\"includeOwn\": \"true\",\n" +
            "\"teachingLanguages\": [\"fi\"],\n" +
            "\"realisationStatuses\": null\n" +
            "}";

        MvcResult result = this.getMvcResult(query);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        List<String> languages = new ArrayList<>();
        languages.add(TeachingLanguage.FI);
        languages.add(TeachingLanguage.SV);
        languages.add(TeachingLanguage.EN);
        searchParams.setTeachingLanguages(languages);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        // test with nonexistent language
        searchParams.setTeachingLanguages(Arrays.asList("ru"));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
    }

    @Test
    public void testFindStudies_filterByRealisationTeachingLanguage_caseInsensitive_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitRealisationEntity realisation1 = new CourseUnitRealisationEntity();
        realisation1.setRealisationId("TOT-1");
        realisation1.setOrganizingOrganisationId("UEF");
        realisation1.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation1.setTeachingLanguage(Arrays.asList("fi"));
        realisation1.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "JAR matikka", "UEF",
            "Keppivirtahepo", coopNetwork, Collections.singletonList(realisation1), null, null);
        courseUnitEntity.setTeachingLanguage(Arrays.asList("FI"));
        courseUnitRepository.update(courseUnitEntity);

        CourseUnitRealisationEntity realisation2 = new CourseUnitRealisationEntity();
        realisation2.setRealisationId("TOT-2");
        realisation2.setOrganizingOrganisationId("UEF");
        realisation2.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation2.setTeachingLanguage(Arrays.asList("SV"));
        realisation2.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "DOR matikka", "UEF",
            "Keppivirtahepo2", coopNetwork, Collections.singletonList(realisation2), null, null);
        courseUnitEntity2.setTeachingLanguage(Arrays.asList("sv"));
        courseUnitRepository.update(courseUnitEntity2);

        CourseUnitRealisationEntity realisation3 = new CourseUnitRealisationEntity();
        realisation3.setRealisationId("TOT-3");
        realisation3.setOrganizingOrganisationId("UEF");
        realisation3.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation3.setTeachingLanguage(Arrays.asList("en"));
        realisation3.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "HÖR matikka", "UEF",
            "Keppivirtahepo3", coopNetwork, Collections.singletonList(realisation3), null, null);
        courseUnitEntity3.setTeachingLanguage(Arrays.asList("EN"));
        courseUnitRepository.update(courseUnitEntity3);

        CourseUnitRealisationEntity realisation4 = new CourseUnitRealisationEntity();
        realisation4.setRealisationId("TOT-4");
        realisation4.setOrganizingOrganisationId("UEF");
        realisation4.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation4.setTeachingLanguage(Arrays.asList("en"));
        realisation4.setStatus(StudyStatus.ACTIVE);

        CompletionOptionEntity completionOption = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem = new AssessmentItemEntity();
        assessmentItem.setRealisations(Collections.singletonList(realisation4));
        completionOption.setAssessmentItems(Collections.singletonList(assessmentItem));

        CourseUnitEntity courseUnitEntity4 = createCourseUnit("CU4", "HAR matikka", "UEF",
            "Keppivirtahepo4", coopNetwork, null, null, null);
        courseUnitEntity4.setTeachingLanguage(Arrays.asList("EN"));
        courseUnitEntity4.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.update(courseUnitEntity4);

        CourseUnitRealisationEntity realisation5 = new CourseUnitRealisationEntity();
        realisation5.setRealisationId("TOT-5");
        realisation5.setOrganizingOrganisationId("UEF");
        realisation5.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation5.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity5 = createCourseUnit("CU5", "HÖR matikka", "UEF",
            "Keppivirtahepo5", coopNetwork, Collections.singletonList(realisation5), null, null);

        CourseUnitRealisationEntity realisation6 = new CourseUnitRealisationEntity();
        realisation6.setRealisationId("TOT-6");
        realisation6.setOrganizingOrganisationId("UEF");
        realisation6.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation6.setStatus(StudyStatus.ACTIVE);

        CompletionOptionEntity completionOption2 = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem2 = new AssessmentItemEntity();
        assessmentItem2.setRealisations(Collections.singletonList(realisation6));
        completionOption2.setAssessmentItems(Collections.singletonList(assessmentItem2));

        CourseUnitEntity courseUnitEntity6 = createCourseUnit("CU6", "HAR matikka", "UEF",
            "Keppivirtahepo6", coopNetwork, null, null, null);
        courseUnitEntity6.setTeachingLanguage(Arrays.asList("EN"));
        courseUnitEntity6.setCompletionOptions(Collections.singletonList(completionOption2));
        courseUnitRepository.update(courseUnitEntity6);

        // the actual realisations
        StudyElementReference cu1Ref = new StudyElementReference(courseUnitEntity.getStudyElementId(),
            courseUnitEntity.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        StudyElementReference cu2Ref = new StudyElementReference(courseUnitEntity2.getStudyElementId(),
            courseUnitEntity2.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        StudyElementReference cu3Ref = new StudyElementReference(courseUnitEntity3.getStudyElementId(),
            courseUnitEntity3.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        StudyElementReference cu4Ref = new StudyElementReference(courseUnitEntity4.getStudyElementId(),
            courseUnitEntity4.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        StudyElementReference cu5Ref = new StudyElementReference(courseUnitEntity5.getStudyElementId(),
            courseUnitEntity5.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        StudyElementReference cu6Ref = new StudyElementReference(courseUnitEntity6.getStudyElementId(),
            courseUnitEntity6.getOrganizingOrganisationId(), fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        RealisationEntity realisationEntity = createRealisation(realisation1.getRealisationId(), realisation1.getOrganizingOrganisationId(),
            Collections.singletonList(cu1Ref), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity2 = createRealisation(realisation2.getRealisationId(), realisation2.getOrganizingOrganisationId(),
            Collections.singletonList(cu2Ref), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity3 = createRealisation(realisation3.getRealisationId(), realisation3.getOrganizingOrganisationId(),
            Collections.singletonList(cu3Ref), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity4 = createRealisation(realisation4.getRealisationId(), realisation4.getOrganizingOrganisationId(),
            Collections.singletonList(cu4Ref), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity5 = createRealisation(realisation5.getRealisationId(), realisation5.getOrganizingOrganisationId(),
            Collections.singletonList(cu5Ref), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity6 = createRealisation(realisation6.getRealisationId(), realisation6.getOrganizingOrganisationId(),
            Collections.singletonList(cu6Ref), Collections.singletonList(coopNetwork));

        String query = "{\n" +
            "\"includeOwn\": \"true\",\n" +
            "\"realisationTeachingLanguages\": [\"fi\"]\n" +
            "}";

        MvcResult result = this.getMvcResult(query);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        List<String> languages = new ArrayList<>();
        languages.add(TeachingLanguage.FI);
        languages.add(TeachingLanguage.SV);
        languages.add(TeachingLanguage.EN);
        searchParams.setRealisationTeachingLanguages(languages);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
        ));

        // test with nonexistent language
        searchParams.setRealisationTeachingLanguages(Arrays.asList("kr"));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(0, actualResult.getResults().size());

        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId()))
        ));

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED, TeachingLanguage.FI));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED, TeachingLanguage.EN));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        // test that we only get the realisations that match
        CourseUnitEntity courseUnitEntity7 = createCourseUnit("CU7", "HAR matikka", "UEF",
            "Keppivirtahepo7", coopNetwork, Arrays.asList(realisation1, realisation2, realisation3, realisation4, realisation5, realisation6), null, null);

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.FI, TeachingLanguage.SV));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitRestDTO cu7 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();
        assertEquals(2, cu7.getRealisations().size());
        assertThat(cu7.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation1.getRealisationId())),
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        // test with both parmaeters, different values
        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.FI));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.SV));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        cu7 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();
        assertEquals(6, cu7.getRealisations().size());
        assertThat(cu7.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation1.getRealisationId())),
            hasProperty("realisationId", is(realisation2.getRealisationId())),
            hasProperty("realisationId", is(realisation3.getRealisationId())),
            hasProperty("realisationId", is(realisation4.getRealisationId())),
            hasProperty("realisationId", is(realisation5.getRealisationId())),
            hasProperty("realisationId", is(realisation6.getRealisationId()))
        ));

        // test with UNSPECIFIED
        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitRestDTO cu5 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity5.getStudyElementId())).findFirst().get();
        cu7 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();

        assertEquals(1, cu5.getRealisations().size());
        assertThat(cu5.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation5.getRealisationId()))
        ));

        assertEquals(2, cu7.getRealisations().size());
        assertThat(cu7.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation5.getRealisationId())),
            hasProperty("realisationId", is(realisation6.getRealisationId()))
        ));

        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.FI));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);
        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitRestDTO cu1 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        cu5 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity5.getStudyElementId())).findFirst().get();
        CourseUnitRestDTO cu6 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity6.getStudyElementId())).findFirst().get();
        cu7 = (CourseUnitRestDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();

        assertEquals(1, cu1.getRealisations().size());
        assertEquals(1, cu5.getRealisations().size());
        assertNull(cu6.getRealisations());
        assertEquals(1, cu6.getAssessmentItems().get(0).getRealisations().size());
        assertEquals(6, cu7.getRealisations().size());
    }

    @Test
    public void testFindStudies_onlyReturnRealisationsFromSameNetwork_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork otherCoopNetwork = createCooperationNetwork("CN-2");

        List<CooperationNetwork> coopNetworks = new ArrayList<>();
        coopNetworks.add(coopNetwork);
        coopNetworks.add(otherCoopNetwork);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");

        // the actual realisations
        RealisationEntity realisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork));

        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(otherCoopNetwork));

        // denormalized realisation info
        CourseUnitRealisationEntity realisationInSameNetwork = new CourseUnitRealisationEntity();
        realisationInSameNetwork.setRealisationId("TOT-1");
        realisationInSameNetwork.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisationInSameNetwork.setOrganizingOrganisationId("UEF");
        realisationInSameNetwork.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity realisationInSameNetwork2 = new CourseUnitRealisationEntity();
        realisationInSameNetwork2.setRealisationId("TOT-2");
        realisationInSameNetwork2.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisationInSameNetwork2.setOrganizingOrganisationId("UEF");
        realisationInSameNetwork2.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity realisationInDifferentNetwork = new CourseUnitRealisationEntity();
        realisationInDifferentNetwork.setRealisationId("TOT-3");
        realisationInDifferentNetwork.setCooperationNetworks(Collections.singletonList(otherCoopNetwork));
        realisationInDifferentNetwork.setOrganizingOrganisationId("UEF");
        realisationInDifferentNetwork.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> realisations = new LinkedList<>();
        realisations.add(realisationInSameNetwork);
        realisations.add(realisationInSameNetwork2);
        realisations.add(realisationInDifferentNetwork);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetworks, realisations, null, null);

        CompletionOptionEntity completionOption = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem = new AssessmentItemEntity();
        assessmentItem.setRealisations(Collections.singletonList(realisationInDifferentNetwork));
        completionOption.setAssessmentItems(Collections.singletonList(assessmentItem));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JAR matikka", "UEF",
            "Keppivirtahepo", coopNetwork, null, null, null);
        courseUnitEntity2.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.update(courseUnitEntity2);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setNetworkIdentifiers(Collections.singletonList("CN-1"));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        for(StudyElementRestDTO studyElement : actualResult.getResults()) {
            CourseUnitRestDTO courseUnitResult = (CourseUnitRestDTO) studyElement;
            if(courseUnitResult.getStudyElementId().equals(courseUnitEntity.getStudyElementId())) {
                assertEquals(2, courseUnitResult.getRealisations().size());
                assertTrue(CollectionUtils.isEmpty(courseUnitResult.getCompletionOptions()));
                assertThat(courseUnitResult.getRealisations(), containsInAnyOrder(
                    hasProperty("realisationId", is(realisationInSameNetwork.getRealisationId())),
                    hasProperty("realisationId", is(realisationInSameNetwork2.getRealisationId()))
                ));
            } else if(courseUnitResult.getStudyElementId().equals(courseUnitEntity2.getStudyElementId())) {
                assertTrue(CollectionUtils.isEmpty(courseUnitResult.getRealisations()));
                assertEquals(1, courseUnitResult.getCompletionOptions().size());
                assertEquals(1, courseUnitResult.getCompletionOptions().get(0).getAssessmentItems().size());
                assertTrue(CollectionUtils.isEmpty(courseUnitResult.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations()));
            }
        }
    }

    @Test
    public void testFindStudies_sortingByFields_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "UEF",
            "Keppivirtahepo", coopNetwork, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setSortBy(StudiesSearchSortField.ID);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        searchParams.setSortDirection(Sort.Direction.DESC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setSortBy(StudiesSearchSortField.IDENTIFIER_CODE);
        searchParams.setSortDirection(Sort.Direction.ASC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setSortBy(StudiesSearchSortField.NAME);
        searchParams.setSortDirection(Sort.Direction.DESC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        searchParams.setSortDirection(Sort.Direction.ASC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setSortBy(StudiesSearchSortField.CREDITS_MAX);
        searchParams.setSortDirection(Sort.Direction.ASC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        searchParams.setSortBy(StudiesSearchSortField.CREDITS_MIN);
        searchParams.setSortDirection(Sort.Direction.DESC);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInRelativeOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));
    }

    /**
     * Test data explained:
     *
     * All requests made as UEF. UEF belongs to CN-1 network.
     *
     * CU1 (network CN-1) has realisation TOT-1 (network CN-1), SHOULD be visible in "enrollableRealisations" aggregation
     * CU2 (networks CN-1, CN-2) has realisation TOT-2 (networks CN-1, CN-2), SHOULD NOT be visible in any aggregations
     * CU3 (network CN-1) has assessment item realisation TOT-3 (networks CN-1). SHOULD be visible in "realisationsEnrollableNextSemester" aggregation
     * CU4 (network CN-1) has realisation TOT-3 (network CN-2), SHOULD be visible in "realisationsEnrollableAfterNextSemester" aggregation
     */
    @Test
    public void testFindStudies_returnsAggregations_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        createNetworkWithOrganisations("CN-2", "LAY", "JYU", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);
        allNetworks.add(coopNetwork2);

        // realisation 1, code TOT-1, network CN-1, organizer UEF. Enrollable in current semester.
        CourseUnitRealisationEntity realisation1EnrollableInCurrentSemester = new CourseUnitRealisationEntity();
        realisation1EnrollableInCurrentSemester.setEnrollmentStartDateTime(ZonedDateTime.now().toOffsetDateTime());
        realisation1EnrollableInCurrentSemester.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0).getEndDateAsOffset());
        realisation1EnrollableInCurrentSemester.setRealisationId("TOT-1");
        realisation1EnrollableInCurrentSemester.setEnrollmentClosed(false);
        realisation1EnrollableInCurrentSemester.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation1EnrollableInCurrentSemester.setOrganizingOrganisationId("UEF");
        realisation1EnrollableInCurrentSemester.setStatus(StudyStatus.ACTIVE);
        realisation1EnrollableInCurrentSemester.setTeachingLanguage(Collections.singletonList("fi"));

        // realisation 2, code TOT-2, networks CN-1 & CN-2, organizer UEF. Enrolment expired.
        CourseUnitRealisationEntity realisation2EnrollmentClosed = new CourseUnitRealisationEntity();
        realisation2EnrollmentClosed.setEnrollmentClosed(true);
        realisation2EnrollmentClosed.setRealisationId("TOT-2");
        realisation2EnrollmentClosed.setCooperationNetworks(allNetworks);
        realisation2EnrollmentClosed.setOrganizingOrganisationId("UEF");
        realisation2EnrollmentClosed.setStatus(StudyStatus.ACTIVE);
        realisation2EnrollmentClosed.setTeachingLanguage(Collections.singletonList("EN"));

        // realisation 3, code TOT-3, network CN-1, organizer TUNI. Enrollable in next semester.
        CourseUnitRealisationEntity realisation3EnrollableInNextSemester = new CourseUnitRealisationEntity();
        realisation3EnrollableInNextSemester.setEnrollmentStartDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1).getStartDateAsOffset());
        realisation3EnrollableInNextSemester.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1).getEndDateAsOffset());
        realisation3EnrollableInNextSemester.setRealisationId("TOT-3");
        realisation3EnrollableInNextSemester.setEnrollmentClosed(false);
        realisation3EnrollableInNextSemester.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation3EnrollableInNextSemester.setOrganizingOrganisationId("TUNI");
        realisation3EnrollableInNextSemester.setStatus(StudyStatus.ACTIVE);
        realisation3EnrollableInNextSemester.setTeachingLanguage(Collections.singletonList("sv"));

        // realisation 4, code TOT-4, network CN-2, organiser JYU. Enrollable after next semester
        CourseUnitRealisationEntity realisation4EnrollableAfterNextSemester = new CourseUnitRealisationEntity();
        realisation4EnrollableAfterNextSemester.setEnrollmentStartDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2).getStartDateAsOffset());
        realisation4EnrollableAfterNextSemester.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2).getEndDateAsOffset());
        realisation4EnrollableAfterNextSemester.setRealisationId("TOT-4");
        realisation4EnrollableAfterNextSemester.setEnrollmentClosed(true);
        realisation4EnrollableAfterNextSemester.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        realisation4EnrollableAfterNextSemester.setOrganizingOrganisationId("JYU");
        realisation4EnrollableAfterNextSemester.setStatus(StudyStatus.ACTIVE);

        // realisation , code TOT-1, network CN-1, organizer UEF. Enrollable in current semester but not right now.
        CourseUnitRealisationEntity realisation5EnrollableLaterInCurrentSemester = new CourseUnitRealisationEntity();
        realisation5EnrollableLaterInCurrentSemester.setEnrollmentStartDateTime(null);
        realisation5EnrollableLaterInCurrentSemester.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0).getEndDateAsOffset());
        realisation5EnrollableLaterInCurrentSemester.setRealisationId("TOT-5");
        realisation5EnrollableLaterInCurrentSemester.setEnrollmentClosed(false);
        realisation5EnrollableLaterInCurrentSemester.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation5EnrollableLaterInCurrentSemester.setOrganizingOrganisationId("UEF");
        realisation5EnrollableLaterInCurrentSemester.setStatus(StudyStatus.ACTIVE);

        // realisation 6, code TOT-1, network CN-1, organizer UEF. Enrollable in current semester.
        CourseUnitRealisationEntity realisation6EnrollableInCurrentSemester = new CourseUnitRealisationEntity();
        realisation6EnrollableInCurrentSemester.setEnrollmentStartDateTime(ZonedDateTime.now().toOffsetDateTime());
        realisation6EnrollableInCurrentSemester.setEnrollmentEndDateTime(SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0).getEndDateAsOffset());
        realisation6EnrollableInCurrentSemester.setRealisationId("TOT-6");
        realisation6EnrollableInCurrentSemester.setEnrollmentClosed(false);
        realisation6EnrollableInCurrentSemester.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation6EnrollableInCurrentSemester.setOrganizingOrganisationId("UEF");
        realisation6EnrollableInCurrentSemester.setStatus(StudyStatus.ACTIVE);

        // course unit references
        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");
        StudyElementReference cu2Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU2", "UEF");
        StudyElementReference cu3Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU3", "TUNI");
        StudyElementReference cu4Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU4", "JYU");
        StudyElementReference cu5Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU5", "UEF");
        StudyElementReference cu6Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU6", "UEF");
        StudyElementReference cu7Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU7", "UEF");

        // the actual realisations
        RealisationEntity realisation1EnrollableInCurrentSemesterEntity = createRealisation(
            realisation1EnrollableInCurrentSemester.getRealisationId(), realisation1EnrollableInCurrentSemester.getOrganizingOrganisationId(),
            Collections.singletonList(cuReference), realisation1EnrollableInCurrentSemester.getCooperationNetworks());

        RealisationEntity realisation2EnrollmentClosedEntity = createRealisation(
            realisation2EnrollmentClosed.getRealisationId(), realisation2EnrollmentClosed.getOrganizingOrganisationId(),
            Collections.singletonList(cu2Reference), realisation2EnrollmentClosed.getCooperationNetworks());

        RealisationEntity realisation3EnrollableInNextSemesterEntity = createRealisation(
            realisation3EnrollableInNextSemester.getRealisationId(), realisation3EnrollableInNextSemester.getOrganizingOrganisationId(),
            Collections.singletonList(cu3Reference), realisation3EnrollableInNextSemester.getCooperationNetworks());

        RealisationEntity realisation4EnrollableAfterNextSemesterEntity = createRealisation(
            realisation4EnrollableAfterNextSemester.getRealisationId(), realisation4EnrollableAfterNextSemester.getOrganizingOrganisationId(),
            Collections.singletonList(cu4Reference), realisation4EnrollableAfterNextSemester.getCooperationNetworks());

        RealisationEntity realisation5EnrollableLaterThisSemesterEntity = createRealisation(
            realisation5EnrollableLaterInCurrentSemester.getRealisationId(), realisation5EnrollableLaterInCurrentSemester.getOrganizingOrganisationId(),
            Collections.singletonList(cu5Reference), realisation5EnrollableLaterInCurrentSemester.getCooperationNetworks());

        RealisationEntity realisation6EnrollableInCurrentSemesterEntity = createRealisation(
            realisation6EnrollableInCurrentSemester.getRealisationId(), realisation6EnrollableInCurrentSemester.getOrganizingOrganisationId(),
            Collections.singletonList(cu6Reference), realisation6EnrollableInCurrentSemester.getCooperationNetworks());

        // course unit 1, code CU1, network CN-1, organizer UEF, with realisation TOT-1.
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, Collections.singletonList(realisation1EnrollableInCurrentSemester), null, null);

        // course unit 2, code CU2, networks CN-1 & CN-2, organizer UEF, with closed realisation TOT-2
        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", allNetworks, Collections.singletonList(realisation2EnrollmentClosed), null, null);

        CompletionOptionEntity completionOption = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem = new AssessmentItemEntity();
        assessmentItem.setRealisations(Collections.singletonList(realisation3EnrollableInNextSemester));
        completionOption.setAssessmentItems(Collections.singletonList(assessmentItem));

        // course unit 3, code CU3, network CN-1, organiser TUNI, with assessment item realisation TOT-3 enrollable in next semester and TOT-3 as normal realisation
        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "TUNI",
            "Keppivirtahepo", coopNetwork, Collections.singletonList(realisation3EnrollableInNextSemester), null, null);
        courseUnitEntity3.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.update(courseUnitEntity3);

        // course unit 4, code CU4, network CN-2, organiser JYU, with realisation TOT-4 enrollable after next semester
        CourseUnitEntity courseUnitEntity4 = createCourseUnit("CU4", "JYU matikka", "JYU",
            "BLAblbabla", coopNetwork2, Collections.singletonList(realisation4EnrollableAfterNextSemester), null, null);

        // course unit 5, code CU4, network CN-2, organiser JYU, with realisation TOT-4 enrollable after next semester
        CourseUnitEntity courseUnitEntity5 = createCourseUnit("CU5", "JYU matikka", "UEF",
            "BLAblbabla", coopNetwork, Collections.singletonList(realisation5EnrollableLaterInCurrentSemester), null, null);

        // course unit 6, no realisations, no networks.
        CourseUnitEntity courseUnitEntity6 = new CourseUnitEntity();
        courseUnitEntity6.setStudyElementId("CU6");
        courseUnitEntity6.setOrganizingOrganisationId("UEF");
        courseUnitEntity6.setStudyElementIdentifierCode("JYU hissa");
        courseUnitRepository.save(courseUnitEntity6);

        // course unit 7, code CU7, network CN-1, organizer UEF, with realisation TOT-6.
        CourseUnitEntity courseUnitEntity7 = createCourseUnit("CU7", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, Collections.singletonList(realisation6EnrollableInCurrentSemester), null, null);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Arrays.asList("UEF", "TUNI", "JYU"));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(6, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        assertThat(actualResult.getAggregations(), containsInAnyOrder(
            hasProperty("name", is("enrollableRealisations")),
            hasProperty("name", is("realisationsEnrollableThisSemester")),
            hasProperty("name", is("realisationsEnrollableNextSemester")),
            hasProperty("name", is("realisationsEnrollableAfterNextSemester")),
            hasProperty("name", is("networks")),
            hasProperty("name", is("types")),
            hasProperty("name", is("organisations")),
            hasProperty("name", is("teachingLanguages")),
            hasProperty("name", is("realisationTeachingLanguages"))
        ));

        for(AggregationDTO dto : actualResult.getAggregations()) {
            if(dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if(multi.getName().equals("enrollableRealisations")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("UEF"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("UEF")) {
                            assertEquals(2, orgBucket.getCount());
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(2, courseUnitBucket.size());
                            assertThat(courseUnitBucket, containsInAnyOrder(
                                hasProperty("key", is(courseUnitEntity.getStudyElementId())),
                                hasProperty("key", is(courseUnitEntity7.getStudyElementId()))
                            ));
                            assertEquals(1, courseUnitBucket.get(0).getCount());
                            assertEquals(1, courseUnitBucket.get(1).getCount());
                        }
                    }
                } else if(multi.getName().equals("realisationsEnrollableThisSemester")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("UEF"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("UEF")) {
                            assertEquals(2, orgBucket.getCount());
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(2, courseUnitBucket.size());
                            assertThat(courseUnitBucket, containsInAnyOrder(
                                hasProperty("key", is(courseUnitEntity.getStudyElementId())),
                                hasProperty("key", is(courseUnitEntity7.getStudyElementId()))
                            ));
                            assertEquals(1, courseUnitBucket.get(0).getCount());
                            assertEquals(1, courseUnitBucket.get(1).getCount());
                        }
                    }
                } else if(multi.getName().equals("realisationsEnrollableNextSemester")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("TUNI"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("TUNI")) {
                            assertEquals(1, orgBucket.getCount());
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(1, courseUnitBucket.size());
                            assertThat(courseUnitBucket, containsInAnyOrder(
                                hasProperty("key", is(courseUnitEntity3.getStudyElementId()))
                            ));
                            assertEquals(2, courseUnitBucket.get(0).getCount());
                        }
                    }
                } else if(multi.getName().equals("realisationsEnrollableAfterNextSemester")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("JYU"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("JYU")) {
                            assertEquals(1, orgBucket.getCount());
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(1, courseUnitBucket.size());
                            assertThat(courseUnitBucket, containsInAnyOrder(
                                hasProperty("key", is(courseUnitEntity4.getStudyElementId()))
                            ));
                            assertEquals(1, courseUnitBucket.get(0).getCount());
                        }
                    }
                } else if(multi.getName().equals("networks")) {
                    assertEquals(2, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("CN-1")),
                        hasProperty("key", is("CN-2"))
                    ));
                } else if(multi.getName().equals("realisationTeachingLanguages")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("ACTIVE"))
                    ));

                    assertEquals(4, multi.getBuckets().get(0).getCount());
                    assertThat(multi.getBuckets().get(0).getBuckets(), containsInAnyOrder(
                        allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(1L))),
                        allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
                        allOf(hasProperty("key", is("en")), hasProperty("count", is(1L))),
                        allOf(hasProperty("key", is("sv")), hasProperty("count", is(1L)))
                    ));
                }
            }
        }
    }

    @Test
    public void testFindStudies_returnsOnlyRealisationsWithEnrollmentOngoing_shouldSucceed() throws Exception {

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        SemesterDatePeriod currentSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0);

        CourseUnitRealisationEntity closedRealisationRef = new CourseUnitRealisationEntity();
        closedRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset().minusMonths(3));
        closedRealisationRef.setEnrollmentEndDateTime(currentSemester.getStartDateAsOffset().minusMonths(1));
        closedRealisationRef.setRealisationId("TOT-1");
        closedRealisationRef.setCooperationNetworks(allNetworks);
        closedRealisationRef.setOrganizingOrganisationId("UEF");
        closedRealisationRef.setStatus(StudyStatus.ACTIVE);
        closedRealisationRef.setTeachingLanguage(Collections.singletonList("ru"));

        CourseUnitRealisationEntity enrollableRealisationRef = new CourseUnitRealisationEntity();
        enrollableRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset());
        enrollableRealisationRef.setEnrollmentEndDateTime(currentSemester.getEndDateAsOffset());
        enrollableRealisationRef.setRealisationId("TOT-2");
        enrollableRealisationRef.setCooperationNetworks(allNetworks);
        enrollableRealisationRef.setOrganizingOrganisationId("UEF");
        enrollableRealisationRef.setStatus(StudyStatus.ACTIVE);
        enrollableRealisationRef.setTeachingLanguage(Collections.singletonList("fi"));

        CourseUnitRealisationEntity archivedRealisationRef = new CourseUnitRealisationEntity();
        archivedRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset());
        archivedRealisationRef.setEnrollmentEndDateTime(currentSemester.getEndDateAsOffset());
        archivedRealisationRef.setRealisationId("TOT-3");
        archivedRealisationRef.setCooperationNetworks(allNetworks);
        archivedRealisationRef.setOrganizingOrganisationId("UEF");
        archivedRealisationRef.setStatus(StudyStatus.ARCHIVED);

        CourseUnitRealisationEntity enrollableAssessmentItemRealisationRef = new CourseUnitRealisationEntity();
        enrollableAssessmentItemRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset());
        enrollableAssessmentItemRealisationRef.setEnrollmentEndDateTime(currentSemester.getEndDateAsOffset());
        enrollableAssessmentItemRealisationRef.setRealisationId("TOT-4");
        enrollableAssessmentItemRealisationRef.setCooperationNetworks(allNetworks);
        enrollableAssessmentItemRealisationRef.setOrganizingOrganisationId("UEF");
        enrollableAssessmentItemRealisationRef.setStatus(StudyStatus.ACTIVE);
        enrollableAssessmentItemRealisationRef.setTeachingLanguage(Collections.singletonList("sv"));

        List<CourseUnitRealisationEntity> allRealisations = new ArrayList<>();
        allRealisations.add(closedRealisationRef);
        allRealisations.add(enrollableRealisationRef);
        allRealisations.add(archivedRealisationRef);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");

        // the actual realisations
        RealisationEntity closedRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), allNetworks);

        RealisationEntity enrollableRealisationEntity = createRealisation("TOT-2", "UEF",
            Collections.singletonList(cuReference), allNetworks);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF",
            Collections.singletonList(cuReference), allNetworks);

        RealisationEntity enrollableAssessmentItemRealisationEntity = createRealisation("TOT-4", "UEF",
            Collections.singletonList(cuReference), allNetworks);

        // course unit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, allRealisations, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnitEntityWithoutRealisations = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setRealisations(Collections.singletonList(enrollableAssessmentItemRealisationRef));

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setAssessmentItems(Collections.singletonList(assessmentItemEntity));

        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));
        courseUnitRepository.update(courseUnitEntity);

        OffsetDateTime enrollmentTime = OffsetDateTime.now();

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeInactive(false);
        searchParams.setIncludeOwn(true);
        searchParams.setLanguage(Language.FI);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));
        searchParams.setOnlyEnrollable(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setPage(0);
        searchParams.setPageSize(25);
        searchParams.setQuery("");
        searchParams.setSortBy(StudiesSearchSortField.IDENTIFIER_CODE);
        searchParams.setSortDirection(Sort.Direction.ASC);
        searchParams.setType(StudiesSearchElementType.COURSE_UNIT);
        searchParams.setRealisationEnrollmentStartDateTimeTo(enrollmentTime);
        searchParams.setRealisationEnrollmentEndDateTimeFrom(enrollmentTime);
        searchParams.setRealisationStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitRestDTO cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(1, cu.getRealisations().size());
        assertEquals(enrollableRealisationEntity.getRealisationId(), cu.getRealisations().get(0).getRealisationId());
        assertEquals(1, cu.getAssessmentItems().size());
        assertEquals(1, cu.getAssessmentItems().get(0).getRealisations().size());
        assertEquals(enrollableAssessmentItemRealisationEntity.getRealisationId(), cu.getAssessmentItems().get(0).getRealisations().get(0).getRealisationId());

        assertThat(actualResult.getAggregations(), containsInAnyOrder(
            hasProperty("name", is("enrollableRealisations")),
            hasProperty("name", is("realisationsEnrollableThisSemester")),
            hasProperty("name", is("realisationsEnrollableNextSemester")),
            hasProperty("name", is("realisationsEnrollableAfterNextSemester")),
            hasProperty("name", is("networks")),
            hasProperty("name", is("types")),
            hasProperty("name", is("organisations")),
            hasProperty("name", is("teachingLanguages")),
            hasProperty("name", is("realisationTeachingLanguages"))
        ));

        for (AggregationDTO dto : actualResult.getAggregations()) {
            if (dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if (multi.getName().equals("enrollableRealisations")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("UEF"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("UEF")) {
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(1, courseUnitBucket.size());
                            assertEquals(courseUnitEntity.getStudyElementId(), courseUnitBucket.get(0).getKey());
                            assertEquals(2L, courseUnitBucket.get(0).getCount());
                        }
                    }
                }

                if(multi.getName().equals("realisationTeachingLanguages")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("ACTIVE"))
                    ));

                    assertEquals(2, multi.getBuckets().get(0).getCount());
                    assertThat(multi.getBuckets().get(0).getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("fi")),
                        hasProperty("key", is("sv"))
                    ));

                    assertEquals(1, multi.getBuckets().get(0).getBuckets().get(0).getCount());
                    assertEquals(1, multi.getBuckets().get(0).getBuckets().get(0).getCount());
                }
            }
        }

        // test that we get the courseunit without realisations when "includeCourseUnitsWithoutActiveRealisations" = true
        searchParams.setIncludeCourseUnitsWithoutActiveRealisations(true);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntityWithoutRealisations.getStudyElementId()))
        ));

        // test that realisationTeachingLanguages aggregate contains other statuses too
        searchParams.setRealisationStatuses(null);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        for (AggregationDTO dto : actualResult.getAggregations()) {
            if (dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if(multi.getName().equals("realisationTeachingLanguages")) {
                    assertEquals(2, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("ACTIVE")),
                        hasProperty("key", is("ARCHIVED"))
                    ));

                    for(BucketDTO status : multi.getBuckets()) {
                        if(status.getKey().equals("ACTIVE")) {
                            assertEquals(2, status.getBuckets().size());
                            assertEquals(2, status.getCount());
                            assertThat(status.getBuckets(), containsInAnyOrder(
                                hasProperty("key", is("fi")),
                                hasProperty("key", is("sv"))
                            ));

                            assertEquals(1, status.getBuckets().get(0).getCount());
                            assertEquals(1, status.getBuckets().get(1).getCount());
                        }

                        if(status.getKey().equals("ARCHIVED")) {
                            assertEquals(1, status.getBuckets().size());
                            assertEquals(1, status.getCount());
                            assertThat(status.getBuckets(), containsInAnyOrder(
                                hasProperty("key", is("unspecified"))
                            ));

                            assertEquals(1, status.getBuckets().get(0).getCount());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testFindStudies_returnsOnlyRealisationsWithMatchingCooperationNetworks_shouldSucceed() throws Exception {

        createNetworkWithOrganisations("aua12345-1234-99ac-cad5-yry8b401roeh", "UEF");
        createNetworkWithOrganisations("feb71594-0297-41ac-aae6-fcb8b20383d5", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("aua12345-1234-99ac-cad5-yry8b401roeh");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("feb71594-0297-41ac-aae6-fcb8b20383d5");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);
        allNetworks.add(coopNetwork2);

        SemesterDatePeriod nextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1);

        CourseUnitRealisationEntity allNetworksRealisationRef = new CourseUnitRealisationEntity();
        allNetworksRealisationRef.setRealisationId("TOT-1");
        allNetworksRealisationRef.setCooperationNetworks(allNetworks);
        allNetworksRealisationRef.setOrganizingOrganisationId("UEF");
        allNetworksRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity network1RealisationRef = new CourseUnitRealisationEntity();
        network1RealisationRef.setRealisationId("TOT-2");
        network1RealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        network1RealisationRef.setOrganizingOrganisationId("UEF");
        network1RealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity network2RealisationRef = new CourseUnitRealisationEntity();
        network2RealisationRef.setRealisationId("TOT-3");
        network2RealisationRef.setEnrollmentStartDateTime(nextSemester.getStartDateAsOffset());
        network2RealisationRef.setEnrollmentEndDateTime(nextSemester.getEndDateAsOffset());
        network2RealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        network2RealisationRef.setOrganizingOrganisationId("UEF");
        network2RealisationRef.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> allRealisations = new ArrayList<>();
        allRealisations.add(allNetworksRealisationRef);
        allRealisations.add(network1RealisationRef);
        allRealisations.add(network2RealisationRef);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");

        // the actual realisations
        RealisationEntity allNetworksRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), allNetworks);

        RealisationEntity network1RealisationEntity = createRealisation("TOT-2", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork));

        RealisationEntity network2RealisationEntity = createRealisation("TOT-3", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork2));

        // course unit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, allRealisations, null, null, Collections.singletonList("fi"));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeInactive(true);
        searchParams.setIncludeOwn(true);
        searchParams.setLanguage(Language.FI);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));
        searchParams.setOnlyEnrollable(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setPage(0);
        searchParams.setPageSize(25);
        searchParams.setSortBy(StudiesSearchSortField.IDENTIFIER_CODE);
        searchParams.setSortDirection(Sort.Direction.ASC);
        searchParams.setTeachingLanguages(null);
        searchParams.setType(StudiesSearchElementType.COURSE_UNIT);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitRestDTO cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(2, cu.getRealisations().size());
        assertThat(cu.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(allNetworksRealisationRef.getRealisationId())),
            hasProperty("realisationId", is(network1RealisationRef.getRealisationId()))
        ));

        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork2.getId()));
        searchParams.setRealisationEnrollmentEndDateTimeFrom(nextSemester.getStartDateAsOffset().minusDays(1));
        searchParams.setRealisationEnrollmentEndDateTimeTo(nextSemester.getEndDateAsOffset().plusDays(1));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(1, cu.getRealisations().size());
        assertThat(cu.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(network2RealisationRef.getRealisationId()))
        ));

        searchParams.setRealisationEnrollmentEndDateTimeFrom(null);
        searchParams.setRealisationEnrollmentEndDateTimeTo(null);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(2, cu.getRealisations().size());
        assertThat(cu.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(allNetworksRealisationRef.getRealisationId())),
            hasProperty("realisationId", is(network2RealisationRef.getRealisationId()))
        ));
    }

    @Test
    public void testFindStudies_returnsOnlyAssessmentItemRealisationsWithMatchingCooperationNetworks_shouldSucceed() throws Exception {

        createNetworkWithOrganisations("aua12345-1234-99ac-cad5-yry8b401roeh", "UEF");
        createNetworkWithOrganisations("feb71594-0297-41ac-aae6-fcb8b20383d5", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("aua12345-1234-99ac-cad5-yry8b401roeh");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("feb71594-0297-41ac-aae6-fcb8b20383d5");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);
        allNetworks.add(coopNetwork2);

        SemesterDatePeriod nextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1);

        CourseUnitRealisationEntity allNetworksRealisationRef = new CourseUnitRealisationEntity();
        allNetworksRealisationRef.setRealisationId("TOT-1");
        allNetworksRealisationRef.setCooperationNetworks(allNetworks);
        allNetworksRealisationRef.setOrganizingOrganisationId("UEF");
        allNetworksRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity network1RealisationRef = new CourseUnitRealisationEntity();
        network1RealisationRef.setRealisationId("TOT-2");
        network1RealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        network1RealisationRef.setOrganizingOrganisationId("UEF");
        network1RealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity network2RealisationRef = new CourseUnitRealisationEntity();
        network2RealisationRef.setRealisationId("TOT-3");
        network2RealisationRef.setEnrollmentStartDateTime(nextSemester.getStartDateAsOffset());
        network2RealisationRef.setEnrollmentEndDateTime(nextSemester.getEndDateAsOffset());
        network2RealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        network2RealisationRef.setOrganizingOrganisationId("UEF");
        network2RealisationRef.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> allRealisations = new ArrayList<>();
        allRealisations.add(allNetworksRealisationRef);
        allRealisations.add(network1RealisationRef);
        allRealisations.add(network2RealisationRef);

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId("ASS1");
        assessmentItemEntity.setRealisations(allRealisations);

        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setAssessmentItems(Collections.singletonList(assessmentItemEntity));

        // course unit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));
        courseUnitRepository.update(courseUnitEntity);

        StudyElementReference aiReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(courseUnitEntity.getStudyElementId(),
            courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        // the actual realisations
        RealisationEntity allNetworksRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(aiReference), allNetworks);

        RealisationEntity network1RealisationEntity = createRealisation("TOT-2", "UEF",
            Collections.singletonList(aiReference), Collections.singletonList(coopNetwork));

        RealisationEntity network2RealisationEntity = createRealisation("TOT-3", "UEF",
            Collections.singletonList(aiReference), Collections.singletonList(coopNetwork2));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeInactive(true);
        searchParams.setIncludeOwn(true);
        searchParams.setLanguage(Language.FI);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));
        searchParams.setOnlyEnrollable(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setPage(0);
        searchParams.setPageSize(25);
        searchParams.setSortBy(StudiesSearchSortField.IDENTIFIER_CODE);
        searchParams.setSortDirection(Sort.Direction.ASC);
        searchParams.setTeachingLanguages(null);
        searchParams.setType(StudiesSearchElementType.COURSE_UNIT);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitRestDTO cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(2, cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations().size());
        assertThat(cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(allNetworksRealisationRef.getRealisationId())),
            hasProperty("realisationId", is(network1RealisationRef.getRealisationId()))
        ));

        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork2.getId()));
        searchParams.setRealisationEnrollmentEndDateTimeFrom(nextSemester.getStartDateAsOffset().minusDays(1));
        searchParams.setRealisationEnrollmentEndDateTimeTo(nextSemester.getEndDateAsOffset().plusDays(1));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(1, cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations().size());
        assertThat(cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(network2RealisationRef.getRealisationId()))
        ));

        searchParams.setRealisationEnrollmentEndDateTimeFrom(null);
        searchParams.setRealisationEnrollmentEndDateTimeTo(null);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        assertEquals(2, cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations().size());
        assertThat(cu.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(allNetworksRealisationRef.getRealisationId())),
            hasProperty("realisationId", is(network2RealisationRef.getRealisationId()))
        ));
    }

    @Test
    public void testFindStudies_emptyQuery_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        SemesterDatePeriod currentSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0);

        CourseUnitRealisationEntity closedRealisationRef = new CourseUnitRealisationEntity();
        closedRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset().minusMonths(3));
        closedRealisationRef.setEnrollmentEndDateTime(currentSemester.getStartDateAsOffset().minusMonths(1));
        closedRealisationRef.setRealisationId("TOT-1");
        closedRealisationRef.setCooperationNetworks(allNetworks);
        closedRealisationRef.setOrganizingOrganisationId("JYU");
        closedRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity enrollableRealisationRef = new CourseUnitRealisationEntity();
        enrollableRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset());
        enrollableRealisationRef.setEnrollmentEndDateTime(currentSemester.getEndDateAsOffset());
        enrollableRealisationRef.setRealisationId("TOT-2");
        enrollableRealisationRef.setCooperationNetworks(allNetworks);
        enrollableRealisationRef.setOrganizingOrganisationId("JYU");
        enrollableRealisationRef.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> allRealisations = new ArrayList<>();
        allRealisations.add(closedRealisationRef);
        allRealisations.add(enrollableRealisationRef);

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "JYU");

        // the actual realisations
        RealisationEntity closedRealisationEntity = createRealisation("TOT-1", "JYU",
            Collections.singletonList(cuReference), allNetworks);

        RealisationEntity enrollableRealisationEntity = createRealisation("TOT-2", "JYU",
            Collections.singletonList(cuReference), allNetworks);

        // course unit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "JYU",
            "Raipatirai", allNetworks, allRealisations, null, null, Collections.singletonList("fi"));

        OffsetDateTime enrollmentTime = OffsetDateTime.now();

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeInactive(false);
        searchParams.setIncludeOwn(false);
        searchParams.setLanguage(Language.FI);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));
        searchParams.setOrganizingOrganisationIdentifiers(null);
        searchParams.setPage(0);
        searchParams.setPageSize(25);
        searchParams.setQuery("");
        searchParams.setSortBy(StudiesSearchSortField.IDENTIFIER_CODE);
        searchParams.setSortDirection(Sort.Direction.ASC);
        searchParams.setTeachingLanguages(null);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitRestDTO cu = (CourseUnitRestDTO) actualResult.getResults().get(0);
        assertEquals(courseUnitEntity.getStudyElementId(), cu.getStudyElementId());
        //assertEquals(1, cu.getRealisations().size());
        //assertEquals(enrollableRealisationEntity.getRealisationId(), cu.getRealisations().get(0).getRealisationId());

        assertThat(actualResult.getAggregations(), containsInAnyOrder(
            hasProperty("name", is("enrollableRealisations")),
            hasProperty("name", is("realisationsEnrollableThisSemester")),
            hasProperty("name", is("realisationsEnrollableNextSemester")),
            hasProperty("name", is("realisationsEnrollableAfterNextSemester")),
            hasProperty("name", is("networks")),
            hasProperty("name", is("types")),
            hasProperty("name", is("organisations")),
            hasProperty("name", is("teachingLanguages")),
            hasProperty("name", is("realisationTeachingLanguages"))
        ));

        for (AggregationDTO dto : actualResult.getAggregations()) {
            if (dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if (multi.getName().equals("enrollableRealisations")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("JYU"))
                    ));
                    for (BucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("JYU")) {
                            List<BucketDTO> courseUnitBucket = orgBucket.getBuckets();
                            assertEquals(1, courseUnitBucket.size());
                            assertEquals(courseUnitEntity.getStudyElementId(), courseUnitBucket.get(0).getKey());
                            assertEquals(1L, courseUnitBucket.get(0).getCount());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testFindStudies_realisationTeachingAggregatesWorkWithOrLogic_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        SemesterDatePeriod currentSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0);

        CourseUnitRealisationEntity closedRealisationRef = new CourseUnitRealisationEntity();
        closedRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset().minusMonths(3));
        closedRealisationRef.setEnrollmentEndDateTime(currentSemester.getStartDateAsOffset().minusMonths(1));
        closedRealisationRef.setRealisationId("TOT-1");
        closedRealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        closedRealisationRef.setOrganizingOrganisationId("UEF");
        closedRealisationRef.setStatus(StudyStatus.ACTIVE);
        closedRealisationRef.setTeachingLanguage(Collections.singletonList("fi"));

        CourseUnitRealisationEntity enrollableRealisationRef = new CourseUnitRealisationEntity();
        enrollableRealisationRef.setEnrollmentStartDateTime(currentSemester.getStartDateAsOffset());
        enrollableRealisationRef.setEnrollmentEndDateTime(currentSemester.getEndDateAsOffset());
        enrollableRealisationRef.setRealisationId("TOT-2");
        enrollableRealisationRef.setCooperationNetworks(Collections.singletonList(coopNetwork));
        enrollableRealisationRef.setOrganizingOrganisationId("UEF");
        enrollableRealisationRef.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity realisation3Ref = new CourseUnitRealisationEntity();
        realisation3Ref.setRealisationId("TOT-3");
        realisation3Ref.setCooperationNetworks(Collections.singletonList(coopNetwork));
        realisation3Ref.setOrganizingOrganisationId("UEF");
        realisation3Ref.setStatus(StudyStatus.ARCHIVED);

        CourseUnitRealisationEntity realisationInOtherNetwork = new CourseUnitRealisationEntity();
        realisationInOtherNetwork.setRealisationId("TOT-4");
        realisationInOtherNetwork.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        realisationInOtherNetwork.setOrganizingOrganisationId("UEF");
        realisationInOtherNetwork.setStatus(StudyStatus.ACTIVE);
        realisationInOtherNetwork.setTeachingLanguage(Collections.singletonList("ru"));

        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");
        StudyElementReference cuReference2 = DtoInitializer.getStudyElementReferenceForCourseUnit("CU2", "UEF");
        StudyElementReference cuReference3 = DtoInitializer.getStudyElementReferenceForCourseUnit("CU3", "UEF");

        // the actual realisations
        RealisationEntity closedRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(cuReference), Collections.singletonList(coopNetwork));

        RealisationEntity enrollableRealisationEntity = createRealisation("TOT-2", "UEF",
            Collections.singletonList(cuReference2), Collections.singletonList(coopNetwork));

        RealisationEntity realisation3Entity = createRealisation("TOT-3", "UEF",
            Collections.singletonList(cuReference3), Collections.singletonList(coopNetwork));

        RealisationEntity realisationInOtherNetworkEntity = createRealisation("TOT-4", "UEF",
            Collections.singletonList(cuReference3), Arrays.asList(coopNetwork, coopNetwork2));

        // course unit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), Collections.singletonList(closedRealisationRef),
            null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), Collections.singletonList(enrollableRealisationRef),
            null, null, null);

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), Arrays.asList(realisation3Ref, realisationInOtherNetwork),
            null, null, Collections.singletonList("fi"));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.FI));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.FI));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        assertThat(actualResult.getAggregations(), containsInAnyOrder(
            hasProperty("name", is("enrollableRealisations")),
            hasProperty("name", is("realisationsEnrollableThisSemester")),
            hasProperty("name", is("realisationsEnrollableNextSemester")),
            hasProperty("name", is("realisationsEnrollableAfterNextSemester")),
            hasProperty("name", is("networks")),
            hasProperty("name", is("types")),
            hasProperty("name", is("organisations")),
            hasProperty("name", is("teachingLanguages")),
            hasProperty("name", is("realisationTeachingLanguages"))
        ));

        for (AggregationDTO dto : actualResult.getAggregations()) {
            if (dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if (multi.getName().equals("realisationsTeachingLanguages")) {
                    assertEquals(2, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("unspecified")),
                        hasProperty("key", is("fi"))
                    ));
                    assertEquals(1, multi.getBuckets().get(0).getCount());
                    assertEquals(1, multi.getBuckets().get(1).getCount());
                }
            }
        }

        // now limit by network and the realisationInOtherNetwork should still not be visible in realisation teaching language aggs
        searchParams.setRealisationTeachingLanguages(null);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        for (AggregationDTO dto : actualResult.getAggregations()) {
            if (dto.getType() == AggregationType.MULTI) {
                MultiBucketAggregationDTO multi = (MultiBucketAggregationDTO) dto;

                if (multi.getName().equals("realisationsTeachingLanguages")) {
                    assertEquals(2, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("unspecified")),
                        hasProperty("key", is("fi"))
                    ));
                    assertEquals(1, multi.getBuckets().get(0).getCount());
                    assertEquals(1, multi.getBuckets().get(1).getCount());
                }
            }
        }
    }

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity activeCourseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity cancelledCourseUnitEntity = createCourseUnit("CU2", "Peruttu Metro", "UEF",
            "Jarh", allNetworks, null, null, null, Collections.singletonList("fi"));
        cancelledCourseUnitEntity.setStatus(StudyStatus.CANCELLED);
        courseUnitRepository.update(cancelledCourseUnitEntity);

        CourseUnitEntity archivedCourseUnitEntity = createCourseUnit("CU3", "Arkistoitu Metro", "UEF",
            "Rooeh", allNetworks, null, null, null, Collections.singletonList("fi"));
        archivedCourseUnitEntity.setStatus(StudyStatus.ARCHIVED);
        courseUnitRepository.update(archivedCourseUnitEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(archivedCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(cancelledCourseUnitEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindCourseUnits_returnsCourseUnitsWithValidRealisationsOnly_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CooperationNetwork outdatedNetworkRef = DtoInitializer.getCooperationNetwork("CN-1", null, true,
            LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));

        CooperationNetwork otherNetwork = createCooperationNetwork("CN-2");

        CourseUnitRealisationEntity networkRefOutdatedCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        networkRefOutdatedCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        networkRefOutdatedCourseUnitRealisationEntity.setRealisationId("TOT-2");
        networkRefOutdatedCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(outdatedNetworkRef));
        networkRefOutdatedCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity statusCancelledCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        statusCancelledCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        statusCancelledCourseUnitRealisationEntity.setRealisationId("TOT-3");
        statusCancelledCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        statusCancelledCourseUnitRealisationEntity.setStatus(StudyStatus.CANCELLED);

        CourseUnitRealisationEntity otherNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        otherNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        otherNetworkCourseUnitRealisationEntity.setRealisationId("TOT-4");
        otherNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(otherNetwork));
        otherNetworkCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> allRealisations = Arrays.asList(activeCourseUnitRealisationEntity, networkRefOutdatedCourseUnitRealisationEntity,
            statusCancelledCourseUnitRealisationEntity, otherNetworkCourseUnitRealisationEntity);

        CourseUnitEntity activeCourseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, allRealisations, null, null, Collections.singletonList("fi"));

        CourseUnitEntity networkRefOutdatedCourseUnitEntity = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(outdatedNetworkRef), null, null, null, Collections.singletonList("fi"));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(networkRefOutdatedCourseUnitEntity.getStudyElementId()))
        ));

        CourseUnitRestDTO activeCourseUnit = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(activeCourseUnitEntity.getStudyElementId()))
            .findAny().get();

        // remember, /studies/courseunits "includeInactive" default value is true. "otherNetworkCourseUnitRealisationEntity" is
        // only returned here because it is the organisations own realisation, although not being in organisation's networks.
        assertEquals(4, activeCourseUnit.getRealisations().size());
        assertThat(activeCourseUnit.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(activeCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(statusCancelledCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(networkRefOutdatedCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(otherNetworkCourseUnitRealisationEntity.getRealisationId()))
        ));

        // no inactives
        requestParams.add("includeInactive", "false");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId()))
        ));

        activeCourseUnit = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(activeCourseUnitEntity.getStudyElementId()))
            .findAny().get();

        assertEquals(2, activeCourseUnit.getRealisations().size());
        assertThat(activeCourseUnit.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(activeCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(statusCancelledCourseUnitRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindCourseUnitRealisations_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        String url = String.format("/api/v8/studies/courseunits/%s/realisations", courseUnitEntity.getStudyElementId());

        StudyElementReference reference = new StudyElementReference("CU1", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF", Collections.singletonList(reference), allNetworks);

        RealisationEntity cancelledRealisationEntity = createRealisation("TOT-2", "UEF", Collections.singletonList(reference), allNetworks);
        cancelledRealisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(cancelledRealisationEntity);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF", Collections.singletonList(reference), allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<Realisation> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(activeRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(archivedRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(cancelledRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindStudyModules_returnsOnlyStudyModulesWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/studymodules";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        StudyModuleEntity activeStudyModuleEntity = createStudyModule("SM1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);

        StudyModuleEntity cancelledStudyModuleEntity = createStudyModule("SM2", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);
        cancelledStudyModuleEntity.setStatus(StudyStatus.CANCELLED);
        studyModuleRepository.update(cancelledStudyModuleEntity);

        StudyModuleEntity archivedStudyModuleEntity = createStudyModule("SM3", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);
        archivedStudyModuleEntity.setStatus(StudyStatus.ARCHIVED);
        studyModuleRepository.update(archivedStudyModuleEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<StudyModule> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeStudyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(archivedStudyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(cancelledStudyModuleEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudyModules_returnsSubElementHierarchy_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/studymodules";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        StudyModuleEntity activeStudyModuleEntity = createStudyModule("SM1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);

        StudyElementReference studyModuleReference = new StudyElementReference("SM1", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.STUDY_MODULE);

        // denormalized realisation info
        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity organiserOutsideOfNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("TUNI");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setRealisationId("TOT-2");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        // courseunit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), null, null, null, Collections.singletonList("fi"));
        courseUnitEntity.setParents(Collections.singletonList(studyModuleReference));
        courseUnitEntity.setRealisations(Arrays.asList(activeCourseUnitRealisationEntity, organiserOutsideOfNetworkCourseUnitRealisationEntity));
        courseUnitRepository.update(courseUnitEntity);

        StudyElementReference courseUnitReference = new StudyElementReference("CU1", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        // actual realisations
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(courseUnitReference), Collections.singletonList(coopNetwork));
        RealisationEntity organiserOutsideOfNetworkRealisationEntity = createRealisation("TOT-2", "TUNI",
            Collections.singletonList(courseUnitReference), Collections.singletonList(coopNetwork2));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<StudyModule> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        assertEquals(1, actualResult.get(0).getSubElements().size());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.get(0).getSubElements().get(0).getStudyElementId());

        CourseUnit subElementCourseUnit = (CourseUnit) actualResult.get(0).getSubElements().get(0);
        assertEquals(1, subElementCourseUnit.getRealisations().size());
        assertEquals(activeRealisationEntity.getRealisationId(), subElementCourseUnit.getRealisations().get(0).getRealisationId());
    }

    @Test
    public void testFindRealisation_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        RealisationEntity realisationEntity = createRealisation("TOT-1", "UEF", null, allNetworks);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("realisationId", "TOT-1");
        requestParams.set("organizingOrganisationId", "UEF");
        MvcResult result = getMvcResult(url, requestParams);

        List<Realisation> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // change realisation status to CANCELLED
        realisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(realisationEntity);

        // now it should not be visible anymore with default params
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());

        // change status param and it should become visible again
        requestParams.set("statuses", "CANCELLED");

        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // and with all params too
        requestParams.set("statuses", "ACTIVE,ARCHIVED,CANCELLED");

        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());
    }

    @Test
    public void testFindRealisations_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations/search";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            null, allNetworks);

        RealisationEntity cancelledRealisationEntity = createRealisation("TOT-2", "UEF",
            null, allNetworks);
        cancelledRealisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(cancelledRealisationEntity);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF",
            null, allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        // just default values
        RealisationSearchParameters searchParams = new RealisationSearchParameters();
        searchParams.setIncludeInactive(true);
        searchParams.setIncludeOwn(true);
        searchParams.setIncludePast(true);
        searchParams.setOngoingEnrollment(false);

        MvcResult result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        List<Realisation> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only archived
        searchParams.setStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ARCHIVED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only cancelled
        searchParams.setStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.CANCELLED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // all statuses
        searchParams.setStatuses(Arrays.asList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.CANCELLED, fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ARCHIVED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(activeRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(archivedRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(cancelledRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindStudies_returnsOnlyStudyElementsWithActiveRealisationsByDefault_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        // CU1 with ACTIVE realisation
        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        StudyElementReference reference = new StudyElementReference("CU1", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF", Collections.singletonList(reference), allNetworks);

        CourseUnitEntity courseUnitWithActiveRealisation = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, Collections.singletonList(activeCourseUnitRealisationEntity),
            null, null, Collections.singletonList("fi"));

        // CU2 with CANCELLED realisation
        CourseUnitRealisationEntity cancelledCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        cancelledCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        cancelledCourseUnitRealisationEntity.setRealisationId("TOT-2");
        cancelledCourseUnitRealisationEntity.setStatus(StudyStatus.CANCELLED);

        StudyElementReference reference2 = new StudyElementReference("CU2", "UEF",
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        RealisationEntity cancelledRealisationEntity = createRealisation("TOT-2", "UEF", Collections.singletonList(reference2), allNetworks);
        cancelledRealisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(cancelledRealisationEntity);

        // CU3 with ARCHIVED realisation
        CourseUnitEntity courseUnitWithCancelledRealisation = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, Collections.singletonList(cancelledCourseUnitRealisationEntity),
            null, null, Collections.singletonList("fi"));

        CourseUnitRealisationEntity archivedCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        archivedCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        archivedCourseUnitRealisationEntity.setRealisationId("TOT-3");
        archivedCourseUnitRealisationEntity.setStatus(StudyStatus.ARCHIVED);

        StudyElementReference reference3 = new StudyElementReference("CU3", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF", Collections.singletonList(reference3), allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        CourseUnitEntity courseUnitWithArchivedRealisation = createCourseUnit("CU3", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, Collections.singletonList(archivedCourseUnitRealisationEntity),
            null, null, Collections.singletonList("fi"));

        // CU4 with ARCHIVED assessment item realisation
        CourseUnitRealisationEntity archivedAssessmentItemCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        archivedAssessmentItemCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        archivedAssessmentItemCourseUnitRealisationEntity.setRealisationId("TOT-4");
        archivedAssessmentItemCourseUnitRealisationEntity.setStatus(StudyStatus.ARCHIVED);

        StudyElementReference reference4 = new StudyElementReference("CU4", "UEF",
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        RealisationEntity archivedAssessmentItemRealisationEntity = createRealisation("TOT-4", "UEF", Collections.singletonList(reference3), allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setRealisations(Collections.singletonList(archivedAssessmentItemCourseUnitRealisationEntity));
        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setAssessmentItems(Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitWithArchivedAssessmentItemRealisation = createCourseUnit("CU4", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null,null, null, Collections.singletonList("fi"));
        courseUnitWithArchivedAssessmentItemRealisation.setCompletionOptions(Collections.singletonList(completionOptionEntity));
        courseUnitRepository.update(courseUnitWithArchivedAssessmentItemRealisation);

        // CU5 without realisations
        CourseUnitEntity courseUnitWithoutRealisations = createCourseUnit("CU5", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null,null, null, Collections.singletonList("fi"));

        // just default values
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeOwn(true);
        searchParameters.setIncludeInactive(true);
        searchParameters.setRealisationStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE));

        MvcResult result = getMvcResult(searchParameters);

        StudiesRestSearchResults actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnitWithActiveRealisation.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // only archived
        searchParameters.setRealisationStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ARCHIVED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId()))
        ));

        // only cancelled
        searchParameters.setRealisationStatuses(Collections.singletonList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.CANCELLED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnitWithCancelledRealisation.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // all statuses
        searchParameters.setRealisationStatuses(Arrays.asList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ARCHIVED,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.CANCELLED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithActiveRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithCancelledRealisation.getStudyElementId()))
        ));

        // no statuses
        searchParameters.setRealisationStatuses(null);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(5, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithActiveRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithCancelledRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithoutRealisations.getStudyElementId()))
        ));

        // with ACTIVE status AND with "includeCourseUnitsWithoutActiveRealisations" = true
        searchParameters.setRealisationStatuses(Arrays.asList(fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyStatus.ACTIVE));
        searchParameters.setIncludeCourseUnitsWithoutActiveRealisations(true);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);

        assertEquals(5, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithActiveRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithCancelledRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithoutRealisations.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudies_pagingAndSimpleSortingWorksAsIntended_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity courseUnit1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit3 = createCourseUnit("CU3", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit4 = createCourseUnit("CU4", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit5 = createCourseUnit("CU5", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        // get only 1 for each page
        StudiesSearchParameters searchParameters = new StudiesSearchParameters();
        searchParameters.setIncludeOwn(true);
        searchParameters.setIncludeInactive(true);
        searchParameters.setPage(0);
        searchParameters.setPageSize(1);
        searchParameters.setSortBy(StudiesSearchSortField.ID);
        searchParameters.setSortDirection(Sort.Direction.ASC);

        MvcResult result = getMvcResult(searchParameters);

        StudiesRestSearchResults actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit1.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(1);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit2.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(2);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit3.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(3);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit4.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(4);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit5.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page, now should return no results
        searchParameters.setPage(5);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesRestSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
    }

    @Test
    public void testFindCourseUnits_pagingWorksAsIntended_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity courseUnit1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit3 = createCourseUnit("CU3", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit4 = createCourseUnit("CU4", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit5 = createCourseUnit("CU5", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        // get only 1 for each page
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("includeInactive", "true");
        requestParams.set("page", "0");
        requestParams.set("pageSize", "1");

        List<String> fetchedIds = new ArrayList<>();

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "1");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "2");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "3");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "4");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page, now should return no results
        requestParams.set("page", "5");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());

        // GOTTA CATCH 'EM ALL
        assertEquals(5, fetchedIds.size());
        assertThat(fetchedIds, containsInAnyOrder(
            courseUnit1.getStudyElementId(),
            courseUnit2.getStudyElementId(),
            courseUnit3.getStudyElementId(),
            courseUnit4.getStudyElementId(),
            courseUnit5.getStudyElementId()
        ));
    }

    @Test
    public void testFindStudies_returnsOnlyStudiesInOwnNetworks_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2= createCooperationNetwork("CN-2");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork2, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        CourseUnitEntity courseUnitEntity4 = createCourseUnit("CU4", "JUH matikka", "UEF",
            "Inaktiivinen eiverkostoja", Collections.emptyList(), null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Arrays.asList("JYU"));
        searchParams.setNetworkIdentifiers(Arrays.asList("CN-2"));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(0, actualResult.getResults().size());

        // now test with inactives included
        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setIncludeInactive(true);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
        ));
    }

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2= createCooperationNetwork("CN-2");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork2, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // get only 1 for each page
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("includeInactive", "true");

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        requestParams.set("organizingOrganisationId", "JYU");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitRealisationsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "UEF", "JYU");
        createNetworkWithOrganisations("CN-3", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");
        CooperationNetwork coopNetwork3 = createCooperationNetwork("CN-3");

        CourseUnitRealisationEntity courseUnitRealisationEntityOutsideOfOwnNetworks = new CourseUnitRealisationEntity();
        courseUnitRealisationEntityOutsideOfOwnNetworks.setRealisationId("R1");
        courseUnitRealisationEntityOutsideOfOwnNetworks.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntityOutsideOfOwnNetworks.setCooperationNetworks(Collections.singletonList(coopNetwork3));
        courseUnitRealisationEntityOutsideOfOwnNetworks.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity courseUnitRealisationEntityInOwnNetwork = new CourseUnitRealisationEntity();
        courseUnitRealisationEntityInOwnNetwork.setRealisationId("R1");
        courseUnitRealisationEntityInOwnNetwork.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntityInOwnNetwork.setCooperationNetworks(Collections.singletonList(coopNetwork));
        courseUnitRealisationEntityInOwnNetwork.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, Collections.singletonList(courseUnitRealisationEntityOutsideOfOwnNetworks), BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, Collections.singletonList(courseUnitRealisationEntityInOwnNetwork), BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork2, Collections.emptyList(), BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // first without params
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        // "courseUnitRealisationEntityOutsideOfOwnNetworks" is only returned here because it's the organistion's own realisation and includeInactive = true by default
        CourseUnitRestDTO courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityOutsideOfOwnNetworks.getRealisationId(), courseUnit1Dto.getRealisations().get(0).getRealisationId());

        CourseUnitRestDTO courseUnit2Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity2.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit2Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityInOwnNetwork.getRealisationId(), courseUnit2Dto.getRealisations().get(0).getRealisationId());

        CourseUnitRestDTO courseUnit3Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity3.getStudyElementId())).findFirst().get();
        assertEquals(0, courseUnit3Dto.getRealisations().size());

        // then with courseUnitId and organizingOrganisationId params. result should be the same.
        requestParams.add("courseUnitId", "CU1");
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityOutsideOfOwnNetworks.getRealisationId(), courseUnit1Dto.getRealisations().get(0).getRealisationId());
    }

    @Test
    public void testFindRealisations_returnsOnlyRealisationsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations/search";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2= createCooperationNetwork("CN-2");

        RealisationEntity realisationEntity = createRealisation("R1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("R2", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity3 = createRealisation("R3", "JYU", null, Collections.singletonList(coopNetwork2));

        RealisationSearchParameters searchParams = new RealisationSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOngoingEnrollment(false);

        MvcResult result = this.getMvcResult(url, objectMapper.writeValueAsString(searchParams));
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        List<Realisation> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));

        searchParams = new RealisationSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOngoingEnrollment(false);
        searchParams.setNetworkIdentifiers(Arrays.asList("CN-2"));

        result = this.getMvcResult(url, objectMapper.writeValueAsString(searchParams));
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindStudies_returnsOnlyStudiesInSpecifiedNetworks_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF", "JYU");
        createNetworkWithOrganisations("CN-2", "UEF", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        List<CooperationNetwork> allNetworks = Arrays.asList(coopNetwork, coopNetwork2);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "TUNI",
            "Keppivirtahepo", coopNetwork2, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        StudyModuleEntity studyModuleEntity = createStudyModule("SM1", "JUH kok", "TUNI",
            "Rapraa", coopNetwork2);
        studyModuleEntity.setCooperationNetworks(allNetworks);
        studyModuleRepository.update(studyModuleEntity);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        searchParams.setNetworkIdentifiers(Collections.singletonList("CN-2"));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudies_returnsNoResultsIfIncludeOwnFalseAndOnlyOwnOrganisationSpecified_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF", "JYU");
        createNetworkWithOrganisations("CN-2", "UEF", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        List<CooperationNetwork> allNetworks = Arrays.asList(coopNetwork, coopNetwork2);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "TUNI",
            "Keppivirtahepo", coopNetwork2, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        StudyModuleEntity studyModuleEntity = createStudyModule("SM1", "JUH kok", "TUNI",
            "Rapraa", coopNetwork2);
        studyModuleEntity.setCooperationNetworks(allNetworks);
        studyModuleRepository.update(studyModuleEntity);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesRestSearchResults actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        searchParams.setIncludeOwn(false);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));
        searchParams.setIncludeOwn(false);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesRestSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
    }

    @Test
    public void testFindRealisation_returnsRealisationsCorrectlyFromOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF", "HAAGAH", "TUNI", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "HAAGAH", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(4, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // UEF's realisation1 as HAAGAH
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity1.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "HAAGAH");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId()))
        ));

        // JYU's realisation4 as TUNI
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity4.getRealisationId());
        requestParams.add("organizingOrganisationId", "JYU");

        result = getMvcResult(url, requestParams, "TUNI");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // TUNI's realisation4 as JYU
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity3.getRealisationId());
        requestParams.add("organizingOrganisationId", "TUNI");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity3.getRealisationId()))
        ));

        // HAAGAH's realisation4 as UEF
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity2.getRealisationId());
        requestParams.add("organizingOrganisationId", "HAAGAH");

        result = getMvcResult(url, requestParams, "UEF");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));
    }

    @Test
    public void testFindRealisation_returnsOwnRealisationsWithoutNetworksCorrectly_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF", null, null);
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(4, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // just default values as TUNI
        requestParams = new LinkedMultiValueMap<>();

        result = getMvcResult(url, requestParams, "TUNI");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // UEF's realisation2 as JYU
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity2.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindRealisation_returnsNoRealisationsFromUnpublishedNetworks_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/realisations";

        NetworkEntity networkEntity = createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        networkEntity.setPublished(false);
        networkRepository.update(networkEntity);

        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF", null, null);
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF. should return only own studies since network is unpublished. /studies/realisations method always returns our own (including inactive) realisations
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));

        // just default values as TUNI. network unpublished, should return only our own again
        requestParams = new LinkedMultiValueMap<>();

        result = getMvcResult(url, requestParams, "TUNI");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity3.getRealisationId()))
        ));

        // UEF's realisation2 as JYU. should not return anything since network is unpublished.
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity1.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());
    }

    // NOTE: At the time of this writing (9/22) this isn't a valid real life case anymore (hasn't been for a long time). No subelements can be set to COURSE_UNITs currently.
    @Test
    public void testFindCourseUnits_returnsSubElementHierarchy_shouldSucceed() throws Exception {
        String url = "/api/v8/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        CourseUnitEntity courseUnitEntity1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        StudyElementReference courseUnit1Reference = new StudyElementReference("CU1", "UEF", fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        // denormalized realisation info
        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity organiserOutsideOfNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("TUNI");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setRealisationId("TOT-2");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        // courseunit
        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), null, null, null, Collections.singletonList("fi"));
        courseUnitEntity2.setParents(Collections.singletonList(courseUnit1Reference));
        courseUnitEntity2.setRealisations(Arrays.asList(activeCourseUnitRealisationEntity, organiserOutsideOfNetworkCourseUnitRealisationEntity));
        courseUnitRepository.update(courseUnitEntity2);

        StudyElementReference courseUnit2Reference = new StudyElementReference("CU2", "UEF", fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType.COURSE_UNIT);

        // actual realisations
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(courseUnit2Reference), Collections.singletonList(coopNetwork));
        RealisationEntity organiserOutsideOfNetworkRealisationEntity = createRealisation("TOT-2", "TUNI",
            Collections.singletonList(courseUnit2Reference), Collections.singletonList(coopNetwork2));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitRestDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        // we now get both courseunits in the result list, filter out other than the one that has subelements
        assertEquals(2, actualResult.size());

        actualResult = actualResult.stream().filter(r -> r.getStudyElementId().equalsIgnoreCase(courseUnitEntity1.getStudyElementId())).collect(Collectors.toList());
        assertEquals(1, actualResult.get(0).getSubElements().size());
        assertEquals(courseUnitEntity2.getStudyElementId(), actualResult.get(0).getSubElements().get(0).getStudyElementId());

        CourseUnitRestDTO subElementCourseUnit = (CourseUnitRestDTO) actualResult.get(0).getSubElements().get(0);
        assertEquals(1, subElementCourseUnit.getRealisations().size());
        assertEquals(activeRealisationEntity.getRealisationId(), subElementCourseUnit.getRealisations().get(0).getRealisationId());
    }

    private MvcResult getMvcResult(StudiesSearchParameters searchParams) throws Exception {
        return this.getMvcResult(objectMapper.writeValueAsString(searchParams));
    }

    private MvcResult getMvcResult(String content) throws Exception {
        return getMvcResult("/api/v8/studies/search", content);
    }

    private MvcResult getMvcResult(String url, MultiValueMap<String, String> params, String org) throws Exception {
        return this.mockMvc.perform(
                get(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", org)
                    .params(params)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }

    private MvcResult getMvcResult(String url, MultiValueMap<String, String> params) throws Exception {
        return getMvcResult(url, params, "UEF");
    }

    private MvcResult getMvcResult(String url, String content) throws Exception {
        return this.mockMvc.perform(
            post(url)
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }

    private NetworkEntity createNetworkWithOrganisations(String networkId, String... organisationTkCodes) {
        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(OffsetDateTime.now().minusYears(1));
        validity.setEnd(OffsetDateTime.now().plusYears(2));

        List<NetworkOrganisation> networkOrganisations = Arrays.asList(organisationTkCodes)
            .stream().map(organisationTkCode -> new NetworkOrganisation(organisationTkCode, true, validity)).collect(Collectors.toList());
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(networkId, null, networkOrganisations, validity, true);

        networkRepository.create(networkEntity);

        return networkEntity;
    }

    private CooperationNetwork createCooperationNetwork(String networkId) {
        return DtoInitializer.getCooperationNetwork(networkId, null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(2));
    }

    private CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, CooperationNetwork coopNetwork, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax) {
        return this.createCourseUnit(courseUnitId, identifierCode, organisationTkCode, name, Collections.singletonList(coopNetwork), realisationRefs, creditsMin, creditsMax);
    }

    private CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, List<CooperationNetwork> coopNetworks, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax) {
        return this.createCourseUnit(courseUnitId, identifierCode, organisationTkCode, name, coopNetworks, realisationRefs, creditsMin, creditsMax, null);
    }

    private CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, List<CooperationNetwork> coopNetworks, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax,
                                              List<String> teachingLanguages) {
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitId, organisationTkCode, coopNetworks,
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString(name, null, null));
        courseUnitEntity.setStudyElementIdentifierCode(identifierCode);
        courseUnitEntity.setOrganisationReferences(
            Collections.singletonList(DtoInitializer.getOrganisationReference(DtoInitializer.getOrganisation(organisationTkCode, organisationTkCode),
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER)));
        courseUnitEntity.setRealisations(realisationRefs);
        courseUnitEntity.setCreditsMax(creditsMax);
        courseUnitEntity.setCreditsMin(creditsMin);
        courseUnitEntity.setTeachingLanguage(teachingLanguages);
        courseUnitEntity.setOrganizingOrganisationId(organisationTkCode);

        courseUnitRepository.create(courseUnitEntity);
        return courseUnitEntity;
    }

    private StudyModuleEntity createStudyModule(String studyModuleId, String identifierCode, String organisationTkCode, String name, CooperationNetwork coopNetwork) {
        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(studyModuleId, identifierCode, organisationTkCode, Collections.singletonList(coopNetwork),
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString(name, null, null));
        studyModuleEntity.setOrganisationReferences(
            Collections.singletonList(DtoInitializer.getOrganisationReference(DtoInitializer.getOrganisation(organisationTkCode, organisationTkCode),
                fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole.ROLE_MAIN_ORGANIZER)));

        studyModuleRepository.create(studyModuleEntity);
        return studyModuleEntity;
    }

    private RealisationEntity createRealisation(String realisationId, String organizingOrganisationTkCode, List<StudyElementReference> studyElements, List<CooperationNetwork> cooperationNetworks) {
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(realisationId, organizingOrganisationTkCode, studyElements, cooperationNetworks);
        realisationRepository.create(realisationEntity);
        return realisationEntity;
    }
}


