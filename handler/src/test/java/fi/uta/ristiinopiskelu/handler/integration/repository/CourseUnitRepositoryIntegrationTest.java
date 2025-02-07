package fi.uta.ristiinopiskelu.handler.integration.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.utils.AggregationUtils;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class CourseUnitRepositoryIntegrationTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitRepositoryIntegrationTest.class);

    @Test
    public void testCreateCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-TIETS2");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );
    }

    @Test
    public void testCreateDuplicateCourseUnit_shouldFail() {
        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-TIETS2");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit, IndexQuery.OpType.CREATE);
        assertThrows(DataIntegrityViolationException.class, () -> courseUnitRepository.create(courseUnit, IndexQuery.OpType.CREATE));
    }

    @Test
    public void testUpdateCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-UPDATE");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);

        CourseUnitEntity courseUnit2 = courseUnitRepository.findById("HY-UPDATE").get();
        courseUnit.setAbbreviation("Tietojärjestelmät MUOKATTU");
        courseUnitRepository.update(courseUnit2);
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

    @Test
    public void testDeleteCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-DELETE");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);

        courseUnitRepository.deleteById("HY-DELETE");
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

    @Test
    public void testAGgs() {
        CooperationNetwork coopNetwork = new CooperationNetwork();
        coopNetwork.setId("CN-1");

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

        CompletionOptionEntity completionOption = new CompletionOptionEntity();
        AssessmentItemEntity assessmentItem = new AssessmentItemEntity();
        assessmentItem.setRealisations(Collections.singletonList(realisation3EnrollableInNextSemester));
        completionOption.setAssessmentItems(Collections.singletonList(assessmentItem));

        // course unit 3, code CU3, network CN-1, organiser TUNI, with assessment item realisation TOT-3 enrollable in next semester and TOT-3 as normal realisation
        CourseUnitEntity courseUnitEntity3 = new CourseUnitEntity();
        courseUnitEntity3.setStudyElementId("CU3");
        courseUnitEntity3.setOrganizingOrganisationId("TUNI");
        courseUnitEntity3.setCompletionOptions(Collections.singletonList(completionOption));
        courseUnitRepository.save(courseUnitEntity3);

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
            .query(new MatchAllQuery.Builder().build()._toQuery())
            .index("opintojaksot");

        searchRequestBuilder.aggregations("testAggregation", agg -> agg
            .nested(na -> na
                .path("completionOptions.assessmentItems.realisations"))
            .aggregations("testTerms", agg2 -> agg2
                .terms(ta -> ta
                    .field("completionOptions.assessmentItems.realisations.realisationId"))));


        SearchResponse<StudyElementEntity> response = elasticsearchTemplate.execute(client -> client.search(searchRequestBuilder.build(), StudyElementEntity.class));

        List<AggregationDTO> mappedAggs = AggregationUtils.mapAggregations(response.aggregations());

        assertEquals(1, response.hits().total().value());
    }

    @Test
    public void testFindByStudyElementReference_shouldReturnMoreThanDefaultMaxSize10Results_shouldSucceed() {
        for(int i = 0; i < 20; i++) {
            CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity("CU1", "TEST", null, new LocalisedString("test", "test", "test"));
            courseUnit.setParents(Collections.singletonList(new StudyElementReference("SM1", "TEST2", StudyElementType.STUDY_MODULE)));
            courseUnitRepository.save(courseUnit);
        }

        List<CourseUnitEntity> results = courseUnitRepository.findByStudyElementReference("SM1", "TEST2", CourseUnitEntity.class);
        assertEquals(20, results.size());
    }
}
