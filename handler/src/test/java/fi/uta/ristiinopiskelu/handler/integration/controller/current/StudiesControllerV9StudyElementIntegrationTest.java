package fi.uta.ristiinopiskelu.handler.integration.controller.current;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchSortField;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleBucketDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleMultiBucketAggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudiesControllerV9StudyElementIntegrationTest extends AbstractStudiesControllerV9IntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Test
    public void testFindStudies_returnsTimestampsAndDatesInCorrectStringFormat_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", null, "UEF",
            "Testijakso", coopNetwork, null, null, null);
        courseUnitEntity.setValidityStartDate(LocalDate.now());
        courseUnitRepository.update(courseUnitEntity);

        StudyModuleEntity studyModuleEntity = createStudyModule("SM1", null, "UEF",
            "Testikokonaisuus", coopNetwork);
        studyModuleEntity.setValidityStartDate(LocalDate.now());
        studyModuleRepository.update(studyModuleEntity);

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());

        JsonNode resultNode = objectMapper.readTree(content);
        ArrayNode resultsNode = (ArrayNode) resultNode.get("results");

        Pattern offsetDateTimePattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{1,3}");
        Pattern localDatePattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

        for(JsonNode studyElementNode : resultsNode) {
            JsonNode sendingTimeNode = studyElementNode.get("sendingTime");
            assertEquals(JsonNodeType.STRING, sendingTimeNode.getNodeType());

            Matcher sendingTimeMatcher = offsetDateTimePattern.matcher(sendingTimeNode.asText());
            assertTrue(sendingTimeMatcher.find());
            
            JsonNode validityStartDateNode = studyElementNode.get("validityStartDate");
            assertEquals(JsonNodeType.STRING, validityStartDateNode.getNodeType());

            Matcher validityStartDateMatcher = localDatePattern.matcher(validityStartDateNode.asText());
            assertTrue(validityStartDateMatcher.matches());
        }
    }

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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams.setType(StudiesSearchElementType.STUDY_MODULE);
        searchParams.setOrganizingOrganisationIdentifiers(Collections.singletonList("UEF"));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.STUDY_MODULE, actualResult.getResults().get(0).getType());
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
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(StudyElementType.COURSE_UNIT, actualResult.getResults().get(0).getType());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());
        assertEquals(courseUnitEntity.getOrganizingOrganisationId(), actualResult.getResults().get(0).getOrganisationReferences().get(0).getOrganisation().getOrganisationTkCode());

        searchParams.setTeachingLanguages(null);
        searchParams.setRealisationTeachingLanguages(null);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntityWithoutTeachingLanguages.getStudyElementId()))
        ));

        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED.getValue()));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());

        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntityWithoutTeachingLanguages.getStudyElementId())),
            hasProperty("studyElementId", is(studyModuleEntity.getStudyElementId()))
        ));

        studyModuleEntity.setTeachingLanguage(Collections.singletonList("SV"));
        studyModuleRepository.update(studyModuleEntity);

        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        searchParams.setOnlyEnrollable(false);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        MvcResult result = this.getMvcResult("/api/v9/studies/search", query);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        List<String> languages = new ArrayList<>();
        languages.add(TeachingLanguage.FI.getValue());
        languages.add(TeachingLanguage.SV.getValue());
        languages.add(TeachingLanguage.EN.getValue());
        searchParams.setTeachingLanguages(languages);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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
        StudyElementReference cu1Ref = new StudyElementReference(courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);
        StudyElementReference cu2Ref = new StudyElementReference(courseUnitEntity2.getStudyElementId(), courseUnitEntity2.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);
        StudyElementReference cu3Ref = new StudyElementReference(courseUnitEntity3.getStudyElementId(), courseUnitEntity3.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);
        StudyElementReference cu4Ref = new StudyElementReference(courseUnitEntity4.getStudyElementId(), courseUnitEntity4.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);
        StudyElementReference cu5Ref = new StudyElementReference(courseUnitEntity5.getStudyElementId(), courseUnitEntity5.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);
        StudyElementReference cu6Ref = new StudyElementReference(courseUnitEntity6.getStudyElementId(), courseUnitEntity6.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);

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

        MvcResult result = this.getMvcResult("/api/v9/studies/search", query);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.SV.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        List<String> languages = new ArrayList<>();
        languages.add(TeachingLanguage.FI.getValue());
        languages.add(TeachingLanguage.SV.getValue());
        languages.add(TeachingLanguage.EN.getValue());
        searchParams.setRealisationTeachingLanguages(languages);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());

        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.UNSPECIFIED.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId()))
        ));

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED.getValue(), TeachingLanguage.FI.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED.getValue(), TeachingLanguage.EN.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.FI.getValue(), TeachingLanguage.SV.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitReadDTO cu7 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();
        assertEquals(2, cu7.getRealisations().size());
        assertThat(cu7.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation1.getRealisationId())),
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        // test with both parmaeters, different values
        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.FI.getValue()));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.SV.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        cu7 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();
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
        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED.getValue()));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitReadDTO cu5 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity5.getStudyElementId())).findFirst().get();
        cu7 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();

        assertEquals(1, cu5.getRealisations().size());
        assertThat(cu5.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation5.getRealisationId()))
        ));

        assertEquals(2, cu7.getRealisations().size());
        assertThat(cu7.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation5.getRealisationId())),
            hasProperty("realisationId", is(realisation6.getRealisationId()))
        ));

        searchParams.setTeachingLanguages(Arrays.asList(TeachingLanguage.FI.getValue()));
        searchParams.setRealisationTeachingLanguages(Arrays.asList(TeachingLanguage.UNSPECIFIED.getValue()));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);
        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity5.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity6.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity7.getStudyElementId()))
        ));

        CourseUnitReadDTO cu1 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        cu5 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity5.getStudyElementId())).findFirst().get();
        CourseUnitReadDTO cu6 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity6.getStudyElementId())).findFirst().get();
        cu7 = (CourseUnitReadDTO) actualResult.getResults().stream().filter(r -> r.getStudyElementId().equals(courseUnitEntity7.getStudyElementId())).findFirst().get();

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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        for(AbstractStudyElementReadDTO studyElement : actualResult.getResults()) {
            CourseUnitReadDTO courseUnitResult = (CourseUnitReadDTO) studyElement;
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

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
            hasProperty("name", is("studyElementsByRealisationTeachingLanguages"))
        ));

        for(SimpleAggregationDTO dto : actualResult.getAggregations()) {
            if(dto.getType() == AggregationType.MULTI) {
                SimpleMultiBucketAggregationDTO multi = (SimpleMultiBucketAggregationDTO) dto;

                if(multi.getName().equals("enrollableRealisations")) {
                    assertEquals(1, multi.getBuckets().size());
                    assertThat(multi.getBuckets(), containsInAnyOrder(
                        hasProperty("key", is("UEF"))
                    ));
                    for (SimpleBucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("UEF")) {
                            assertEquals(2, orgBucket.getCount());
                            List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
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
                    for (SimpleBucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("UEF")) {
                            assertEquals(2, orgBucket.getCount());
                            List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
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
                    for (SimpleBucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("TUNI")) {
                            assertEquals(1, orgBucket.getCount());
                            List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
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
                    for (SimpleBucketDTO orgBucket : multi.getBuckets()) {
                        if (orgBucket.getKey().equals("JYU")) {
                            assertEquals(1, orgBucket.getCount());
                            List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
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
                }
            }
        }

        SimpleMultiBucketAggregationDTO aggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(aggregation);

        assertEquals(4, aggregation.getBuckets().size());
        assertThat(aggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("sv")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("en")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(3L)))
        ));
    }

    /**
     * Test data explained:
     *
     * CU1
     * - realisations:
     *    - TOT1 (fi)
     *    - TOT4 (unspecified)
     *    - TOT6 (sv, ru)
     * - assessmentItemRealisations:
     *    - TOT6 (sv, ru)
     *
     * CU2
     * - realisations:
     *    - TOT2 (en)
     *    - TOT5 (unspecified)
     *    - TOT7 (fi, sv, ru)
     *
     * CU3
     * - realisations:
     *    - TOT3 (sv)
     *    - TOT5 (unspecified)
     *    - TOT6 (sv, ru)
     *
     * CU4
     * - realisations:
     *    - TOT3 (sv)
     *    - TOT4 (unspecified)
     *    - TOT1 (fi)
     * - assessmentItemRealisations:
     *    - TOT7 (fi, sv, ru)
     *
     * Final result for "studyElementsByRealisationTeachingLanguages" aggregation should be:
     * - FI: 3
     * - SV: 4
     * - EN: 1
     * - RU: 4
     * - UNSPECIFIED: 4
     */
    @Test
    public void testFindStudies_returnsStudyElementsByRealisationTeachingLanguagesAggregationCorrectly_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitRealisationEntity realisation1CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation1CourseUnitRealisationEntity.setRealisationId("TOT-1");
        realisation1CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation1CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation1CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);
        realisation1CourseUnitRealisationEntity.setTeachingLanguage(Collections.singletonList("fi"));

        CourseUnitRealisationEntity realisation2CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation2CourseUnitRealisationEntity.setRealisationId("TOT-2");
        realisation2CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation2CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation2CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);
        realisation2CourseUnitRealisationEntity.setTeachingLanguage(Collections.singletonList("EN"));

        CourseUnitRealisationEntity realisation3CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation3CourseUnitRealisationEntity.setRealisationId("TOT-3");
        realisation3CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation3CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation3CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);
        realisation3CourseUnitRealisationEntity.setTeachingLanguage(Collections.singletonList("sv"));

        CourseUnitRealisationEntity realisation4CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation4CourseUnitRealisationEntity.setRealisationId("TOT-4");
        realisation4CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation4CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation4CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity realisation5CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation5CourseUnitRealisationEntity.setRealisationId("TOT-5");
        realisation5CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation5CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation5CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity realisation6CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation6CourseUnitRealisationEntity.setRealisationId("TOT-6");
        realisation6CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation6CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation6CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);
        realisation6CourseUnitRealisationEntity.setTeachingLanguage(Arrays.asList(TeachingLanguage.SV.getValue(), TeachingLanguage.RU.getValue()));

        CourseUnitRealisationEntity realisation7CourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        realisation7CourseUnitRealisationEntity.setRealisationId("TOT-7");
        realisation7CourseUnitRealisationEntity.setCooperationNetworks(allNetworks);
        realisation7CourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        realisation7CourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);
        realisation7CourseUnitRealisationEntity.setTeachingLanguage(Arrays.asList(TeachingLanguage.FI.getValue(), TeachingLanguage.SV.getValue(), TeachingLanguage.RU.getValue()));

        // course unit references
        StudyElementReference cuReference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");
        StudyElementReference cu2Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU2", "UEF");
        StudyElementReference cu3Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU3", "UEF");
        StudyElementReference cu4Reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU4", "UEF");

        // the actual realisations
        RealisationEntity realisation1Entity = createRealisation(
            realisation1CourseUnitRealisationEntity.getRealisationId(), realisation1CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Collections.singletonList(cuReference), realisation1CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation2Entity = createRealisation(
            realisation2CourseUnitRealisationEntity.getRealisationId(), realisation2CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cu2Reference, cu3Reference), realisation2CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation3Entity = createRealisation(
            realisation3CourseUnitRealisationEntity.getRealisationId(), realisation3CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cu3Reference, cu4Reference), realisation3CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation4Entity = createRealisation(
            realisation4CourseUnitRealisationEntity.getRealisationId(), realisation4CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cuReference, cu4Reference), realisation4CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation5Entity = createRealisation(
            realisation5CourseUnitRealisationEntity.getRealisationId(), realisation5CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cu2Reference, cu3Reference), realisation5CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation6Entity = createRealisation(
            realisation6CourseUnitRealisationEntity.getRealisationId(), realisation6CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cu3Reference), realisation6CourseUnitRealisationEntity.getCooperationNetworks());

        RealisationEntity realisation7Entity = createRealisation(
            realisation7CourseUnitRealisationEntity.getRealisationId(), realisation7CourseUnitRealisationEntity.getOrganizingOrganisationId(),
            Arrays.asList(cu2Reference), realisation7CourseUnitRealisationEntity.getCooperationNetworks());
        
        CompletionOptionEntity completionOption1 = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem1 = new AssessmentItemEntity();
        assessmentItem1.setRealisations(Collections.singletonList(realisation7CourseUnitRealisationEntity));
        completionOption1.setAssessmentItems(Collections.singletonList(assessmentItem1));

        CompletionOptionEntity completionOption2 = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem2 = new AssessmentItemEntity();
        assessmentItem2.setRealisations(Collections.singletonList(realisation6CourseUnitRealisationEntity));
        completionOption2.setAssessmentItems(Collections.singletonList(assessmentItem2));

        CourseUnitEntity courseUnitEntity1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, Arrays.asList(realisation1CourseUnitRealisationEntity, realisation4CourseUnitRealisationEntity,
                realisation6CourseUnitRealisationEntity), null, null);
        courseUnitEntity1.setCompletionOptions(Collections.singletonList(completionOption2));
        courseUnitRepository.update(courseUnitEntity1);

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", allNetworks, Arrays.asList(realisation2CourseUnitRealisationEntity, realisation5CourseUnitRealisationEntity,
                realisation7CourseUnitRealisationEntity), null, null);

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "UEF",
            "Keppivirtahepo", allNetworks, Arrays.asList(realisation3CourseUnitRealisationEntity, realisation5CourseUnitRealisationEntity,
                realisation6CourseUnitRealisationEntity), null, null);

        CourseUnitEntity courseUnitEntity4 = createCourseUnit("CU4", "JYU matikka", "UEF",
            "BLAblbabla", allNetworks, Arrays.asList(realisation1CourseUnitRealisationEntity, realisation3CourseUnitRealisationEntity,
                realisation4CourseUnitRealisationEntity), null, null);
        courseUnitEntity4.setCompletionOptions(Collections.singletonList(completionOption1));
        courseUnitRepository.update(courseUnitEntity4);
        
        // do the search
        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Arrays.asList("UEF"));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(4, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity1.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
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
            hasProperty("name", is("studyElementsByRealisationTeachingLanguages"))
        ));
        
        SimpleMultiBucketAggregationDTO aggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(aggregation);

        assertEquals(5, aggregation.getBuckets().size());
        assertThat(aggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(3L))),
            allOf(hasProperty("key", is("sv")), hasProperty("count", is(4L))),
            allOf(hasProperty("key", is("en")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("ru")), hasProperty("count", is(4L))),
            allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(4L)))
        ));
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
        searchParams.setRealisationStatuses(Collections.singletonList(StudyStatus.ACTIVE));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitReadDTO cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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
            hasProperty("name", is("studyElementsByRealisationTeachingLanguages"))
        ));

        SimpleMultiBucketAggregationDTO enrollableRealisationsAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("enrollableRealisations")).findAny().get();
        assertNotNull(enrollableRealisationsAggregation);

        assertEquals(1, enrollableRealisationsAggregation.getBuckets().size());

        assertThat(enrollableRealisationsAggregation.getBuckets(), containsInAnyOrder(
            hasProperty("key", is("UEF"))
        ));

        for (SimpleBucketDTO orgBucket : enrollableRealisationsAggregation.getBuckets()) {
            if (orgBucket.getKey().equals("UEF")) {
                List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
                assertEquals(1, courseUnitBucket.size());
                assertEquals(courseUnitEntity.getStudyElementId(), courseUnitBucket.get(0).getKey());
                assertEquals(2L, courseUnitBucket.get(0).getCount());
            }
        }

        SimpleMultiBucketAggregationDTO studyElementsByRealisationTeachingLanguagesAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(studyElementsByRealisationTeachingLanguagesAggregation);

        assertEquals(2, studyElementsByRealisationTeachingLanguagesAggregation.getBuckets().size());
        assertThat(studyElementsByRealisationTeachingLanguagesAggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("sv")), hasProperty("count", is(1L)))
        ));

        // test that we get the courseunit without realisations when "includeCourseUnitsWithoutActiveRealisations" = true
        searchParams.setIncludeCourseUnitsWithoutActiveRealisations(true);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntityWithoutRealisations.getStudyElementId()))
        ));

        // test with realisationStatuses null
        searchParams.setRealisationStatuses(null);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        studyElementsByRealisationTeachingLanguagesAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(studyElementsByRealisationTeachingLanguagesAggregation);

        assertEquals(3, studyElementsByRealisationTeachingLanguagesAggregation.getBuckets().size());
        assertThat(studyElementsByRealisationTeachingLanguagesAggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("sv")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(1L)))
        ));
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitReadDTO cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitReadDTO cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());

        CourseUnitReadDTO cu = (CourseUnitReadDTO) actualResult.getResults().get(0);
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
            hasProperty("name", is("studyElementsByRealisationTeachingLanguages"))
        ));

        SimpleMultiBucketAggregationDTO enrollableRealisationsAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("enrollableRealisations")).findAny().get();
        assertNotNull(enrollableRealisationsAggregation);

        assertEquals(1, enrollableRealisationsAggregation.getBuckets().size());

        assertThat(enrollableRealisationsAggregation.getBuckets(), containsInAnyOrder(
            hasProperty("key", is("JYU"))
        ));
        for (SimpleBucketDTO orgBucket : enrollableRealisationsAggregation.getBuckets()) {
            if (orgBucket.getKey().equals("JYU")) {
                List<SimpleBucketDTO> courseUnitBucket = orgBucket.getBuckets();
                assertEquals(1, courseUnitBucket.size());
                assertEquals(courseUnitEntity.getStudyElementId(), courseUnitBucket.get(0).getKey());
                assertEquals(1L, courseUnitBucket.get(0).getCount());
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
        searchParams.setTeachingLanguages(Collections.singletonList(TeachingLanguage.FI.getValue()));
        searchParams.setRealisationTeachingLanguages(Collections.singletonList(TeachingLanguage.FI.getValue()));

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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
            hasProperty("name", is("studyElementsByRealisationTeachingLanguages"))
        ));

        SimpleMultiBucketAggregationDTO studyElementsByRealisationTeachingLanguagesAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(studyElementsByRealisationTeachingLanguagesAggregation);

        assertEquals(3, studyElementsByRealisationTeachingLanguagesAggregation.getBuckets().size());
        assertThat(studyElementsByRealisationTeachingLanguagesAggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("ru")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(1L)))
        ));

        // now limit by network and the realisationInOtherNetwork should still not be visible in realisation teaching language aggs
        searchParams.setRealisationTeachingLanguages(null);
        searchParams.setNetworkIdentifiers(Collections.singletonList(coopNetwork.getId()));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        studyElementsByRealisationTeachingLanguagesAggregation = (SimpleMultiBucketAggregationDTO) actualResult.getAggregations().stream()
            .filter(agg -> agg.getName().equals("studyElementsByRealisationTeachingLanguages")).findAny().get();
        assertNotNull(studyElementsByRealisationTeachingLanguagesAggregation);

        assertEquals(2, studyElementsByRealisationTeachingLanguagesAggregation.getBuckets().size());
        assertThat(studyElementsByRealisationTeachingLanguagesAggregation.getBuckets(), containsInAnyOrder(
            allOf(hasProperty("key", is("fi")), hasProperty("count", is(1L))),
            allOf(hasProperty("key", is("unspecified")), hasProperty("count", is(1L)))
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

        StudyElementReference reference = new StudyElementReference("CU1", "UEF", StudyElementType.COURSE_UNIT);
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF", Collections.singletonList(reference), allNetworks);

        CourseUnitEntity courseUnitWithActiveRealisation = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, Collections.singletonList(activeCourseUnitRealisationEntity),
            null, null, Collections.singletonList("fi"));

        // CU2 with CANCELLED realisation
        CourseUnitRealisationEntity cancelledCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        cancelledCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        cancelledCourseUnitRealisationEntity.setRealisationId("TOT-2");
        cancelledCourseUnitRealisationEntity.setStatus(StudyStatus.CANCELLED);

        StudyElementReference reference2 = new StudyElementReference("CU2", "UEF", StudyElementType.COURSE_UNIT);

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

        StudyElementReference reference3 = new StudyElementReference("CU3", "UEF", StudyElementType.COURSE_UNIT);

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

        StudyElementReference reference4 = new StudyElementReference("CU4", "UEF", StudyElementType.COURSE_UNIT);

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
        searchParameters.setRealisationStatuses(Collections.singletonList(StudyStatus.ACTIVE));

        MvcResult result = getMvcResult(searchParameters);

        StudiesSearchResults actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnitWithActiveRealisation.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // only archived
        searchParameters.setRealisationStatuses(Collections.singletonList(StudyStatus.ARCHIVED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

        assertEquals(2, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId()))
        ));

        // only cancelled
        searchParameters.setRealisationStatuses(Collections.singletonList(StudyStatus.CANCELLED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnitWithCancelledRealisation.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // all statuses
        searchParameters.setRealisationStatuses(Arrays.asList(StudyStatus.ACTIVE, StudyStatus.ARCHIVED, StudyStatus.CANCELLED));

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

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

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

        assertEquals(5, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitWithArchivedAssessmentItemRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithActiveRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithArchivedRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithCancelledRealisation.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitWithoutRealisations.getStudyElementId()))
        ));

        // with ACTIVE status AND with "includeCourseUnitsWithoutActiveRealisations" = true
        searchParameters.setRealisationStatuses(Arrays.asList(StudyStatus.ACTIVE));
        searchParameters.setIncludeCourseUnitsWithoutActiveRealisations(true);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);

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

        StudiesSearchResults actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit1.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(1);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit2.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(2);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit3.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(3);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit4.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page
        searchParameters.setPage(4);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertEquals(courseUnit5.getStudyElementId(), actualResult.getResults().get(0).getStudyElementId());

        // next page, now should return no results
        searchParameters.setPage(5);
        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
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
        searchParams.setIncludeInactive(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
        ));

        searchParams.setIncludeOwn(false);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());

        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOrganizingOrganisationIdentifiers(Arrays.asList("JYU"));
        searchParams.setNetworkIdentifiers(Arrays.asList("CN-2"));

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());

        // now test with inactives included
        searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setIncludeInactive(true);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity4.getStudyElementId()))
        ));
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
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

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
    }

    @Test
    public void testFindStudies_returnsOwnStudiesWhenNoNetworks_shouldSucceed() throws Exception {
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.emptyList(), null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JAR matikka", "TUNI",
            "Keppivirtahepo", Collections.emptyList(), null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setIncludeInactive(true);
        searchParams.setIncludeCourseUnitsWithoutActiveRealisations(true);

        MvcResult result = this.getMvcResult(searchParams);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setIncludeOwn(false);

        result = this.getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(0, actualResult.getResults().size());
    }

    @Test
    public void testFindStudies_filterByMinEduGuidanceAreas_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "JAR matikka", "UEF",
            "Keppivirtahepo", coopNetwork, null, null, null);
        courseUnitEntity.setMinEduGuidanceArea(List.of(MinEduGuidanceArea.MEDICAL_SCIENCE, MinEduGuidanceArea.EDUCATION));
        courseUnitRepository.update(courseUnitEntity);

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "DOR matikka", "UEF",
            "Keppivirtahepo2", coopNetwork, null, null, null);
        courseUnitEntity2.setMinEduGuidanceArea(List.of(MinEduGuidanceArea.ARTS_AND_CULTURE, MinEduGuidanceArea.EDUCATION));
        courseUnitRepository.update(courseUnitEntity2);

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "HÖR matikka", "UEF",
            "Keppivirtahepo3", coopNetwork, null, null, null);
        courseUnitEntity3.setMinEduGuidanceArea(List.of(MinEduGuidanceArea.HEALTH_AND_WELFARE, MinEduGuidanceArea.EDUCATION, MinEduGuidanceArea.AGRICULTURE_AND_FORESTY));
        courseUnitRepository.update(courseUnitEntity3);

        String query = """
            {
                "includeOwn": "true",
                "minEduGuidanceAreas": [%s]
            }""".formatted(MinEduGuidanceArea.EDUCATION.getCode());

        MvcResult result = this.getMvcResult("/api/v9/studies/search", query);
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        StudiesSearchResults actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        StudiesSearchParameters searchParams = new StudiesSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setMinEduGuidanceAreas(List.of(MinEduGuidanceArea.AGRICULTURE_AND_FORESTY));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        searchParams.setMinEduGuidanceAreas(List.of(MinEduGuidanceArea.MEDICAL_SCIENCE));

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(1, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        searchParams.setMinEduGuidanceAreas(null);

        result = getMvcResult(searchParams);
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(content, StudiesSearchResults.class);
        assertEquals(3, actualResult.getResults().size());
        assertThat(actualResult.getResults(), containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));
    }
}
