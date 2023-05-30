package fi.uta.ristiinopiskelu.persistence.querybuilder;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Predicate;

public class StudiesNativeSearchQueryBuilder {

    public SearchSourceBuilder get(BoolQueryBuilder studiesQueryBuilder, StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                                           StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                                           PageRequest paging, StudiesSearchParameters searchParams) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(studiesQueryBuilder)
            .from((int) paging.getOffset())
            .size(paging.getPageSize());

        if(paging.getSort().isSorted()) {
            paging.getSort().stream().forEach(order -> {
                searchSourceBuilder.sort(order.getProperty(), SortOrder.fromString(order.getDirection().name()));
            });
        }

        String[] searchParamNetworks = searchParams.getActualNetworkIdsUsedInSearch().toArray(new String[0]);

        searchSourceBuilder.aggregation(AggregationBuilders.terms("teachingLanguages")
            .field("teachingLanguage.lowercase")
            .missing(TeachingLanguage.UNSPECIFIED.getValue())
            .size(400));

        BoolQueryBuilder realisationsWithTeachingLanguagesQuery =
            realisationQueriesWithTeachingLanguage == null ? null : realisationQueriesWithTeachingLanguage.getAggregationRealisationQuery();
        BoolQueryBuilder realisationsWithoutTeachingLanguagesQuery =
            realisationQueriesWithoutTeachingLanguage == null ? null : realisationQueriesWithoutTeachingLanguage.getAggregationRealisationQuery();
        BoolQueryBuilder assessmentItemRealisationsWithTeachingLanguagesQuery =
            realisationQueriesWithTeachingLanguage == null ? null : realisationQueriesWithTeachingLanguage.getAggregationAssessmentItemRealisationQuery();
        BoolQueryBuilder assessmentItemRealisationsWithoutTeachingLanguagesQuery =
            realisationQueriesWithoutTeachingLanguage == null ? null : realisationQueriesWithoutTeachingLanguage.getAggregationAssessmentItemRealisationQuery();

        RealisationTeachingLanguageFilters realisationTeachingLanguageFilter = getRealisationTeachingLanguageFilter(searchParams,
            realisationsWithTeachingLanguagesQuery, realisationsWithoutTeachingLanguagesQuery, "realisations", searchParamNetworks);

        RealisationTeachingLanguageFilters assessmentItemRealisationTeachingLanguageFilter = getRealisationTeachingLanguageFilter(searchParams,
            assessmentItemRealisationsWithTeachingLanguagesQuery, assessmentItemRealisationsWithoutTeachingLanguagesQuery,
            "completionOptions.assessmentItems.realisations", searchParamNetworks);

        // use the same realisation queries here to filter the realisation aggregates so that results are in sync with the actual search
        AbstractAggregationBuilder realisationTeachingLanguagesAggregate = aggregateRealisationTeachingLanguages("realisationTeachingLanguages",
            realisationTeachingLanguageFilter, searchParamNetworks, paging.getPageSize());
        searchSourceBuilder.aggregation(realisationTeachingLanguagesAggregate);

        // here too
        AbstractAggregationBuilder assessmentItemRealisationTeachingLanguagesAggregate = aggregateAssessmentItemRealisationTeachingLanguages("assessmentItemRealisationTeachingLanguages",
            assessmentItemRealisationTeachingLanguageFilter, searchParamNetworks, paging.getPageSize());
        searchSourceBuilder.aggregation(assessmentItemRealisationTeachingLanguagesAggregate);

        if(!ArrayUtils.isEmpty(searchParamNetworks)) {
            searchSourceBuilder.aggregation(AggregationBuilders.nested("networks", "cooperationNetworks").subAggregation(
                AggregationBuilders.terms("id").field("cooperationNetworks.id").size(searchParamNetworks.length)
                    .includeExclude(new IncludeExclude(searchParamNetworks, null))));
        }

        searchSourceBuilder.aggregation(AggregationBuilders.terms("organisations").field("organizingOrganisationId").size(searchParams.getOrganisationAmount()));

        searchSourceBuilder.aggregation(AggregationBuilders.terms("types").field("type"));

        SemesterDatePeriod currentSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0);
        final SemesterDatePeriod currentSemesterStartingToday = new SemesterDatePeriod(OffsetDateTime.now(), currentSemester.getEndDateAsOffset());

        String[] currentSemesterValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
            .filter(isValidNetworkDuringDatePeriod(currentSemesterStartingToday))
            .map(NetworkEntity::getId)
            .toArray(String[]::new);

        final SemesterDatePeriod nextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1);
        String[] nextSemesterValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
            .filter(isValidNetworkDuringDatePeriod(nextSemester))
            .map(NetworkEntity::getId)
            .toArray(String[]::new);

        final SemesterDatePeriod semesterAfterNextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2);
        String[] semesterAfterNextValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
            .filter(isValidNetworkDuringDatePeriod(semesterAfterNextSemester))
            .map(NetworkEntity::getId)
            .toArray(String[]::new);

        String[] allowedRealisationStudyStatuses = new String[] { StudyStatus.ACTIVE.name() };

        searchSourceBuilder.aggregation(aggregateRealisationsByOrganizingOrganisationAndStudyElement(
                "enrollableRealisations",
                this.filterByRealisationEnrollmentNotClosed("realisations", allowedRealisationStudyStatuses,
                    searchParamNetworks), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchSourceBuilder.aggregation(aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
            "enrollableAssessmentItemRealisations",
                this.filterByRealisationEnrollmentNotClosed("completionOptions.assessmentItems.realisations",
                    allowedRealisationStudyStatuses, searchParamNetworks), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchSourceBuilder.aggregation(aggregateRealisationsByOrganizingOrganisationAndStudyElement(
            "realisationsEnrollableThisSemester",
                this.filterByRealisationEnrollmentStartDateTime("realisations", currentSemesterStartingToday,
                    allowedRealisationStudyStatuses, currentSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchSourceBuilder.aggregation(aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
            "assessmentItemRealisationsEnrollableThisSemester",
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    currentSemesterStartingToday, allowedRealisationStudyStatuses, currentSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchSourceBuilder.aggregation(aggregateRealisationsByOrganizingOrganisationAndStudyElement(
            "realisationsEnrollableNextSemester",
                this.filterByRealisationEnrollmentStartDateTime("realisations", nextSemester, allowedRealisationStudyStatuses,
                    nextSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchSourceBuilder.aggregation(aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
            "assessmentItemRealisationsEnrollableNextSemester",
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    nextSemester, allowedRealisationStudyStatuses, nextSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchSourceBuilder.aggregation(aggregateRealisationsByOrganizingOrganisationAndStudyElement(
            "realisationsEnrollableAfterNextSemester",
                this.filterByRealisationEnrollmentStartDateTime("realisations", semesterAfterNextSemester,
                    allowedRealisationStudyStatuses, semesterAfterNextValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchSourceBuilder.aggregation(aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
            "assessmentItemRealisationsEnrollableAfterNextSemester",
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    semesterAfterNextSemester, allowedRealisationStudyStatuses, semesterAfterNextValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        return searchSourceBuilder;
    }

    private Predicate<NetworkEntity> isValidNetworkDuringDatePeriod(SemesterDatePeriod datePeriod) {
        return network -> (network.isPublished() &&
            (network.getDeleted() == null || (network.getDeleted() != null && !network.getDeleted()))) &&
            ((network.getValidity().getContinuity() == Validity.ContinuityEnum.INDEFINITELY &&
                ((network.getValidity().getStart() == null && network.getValidity().getEnd() == null) ||
                    DateUtils.isBeforeOrEqual(network.getValidity().getStart(), datePeriod.getStartDateAsOffset()))) ||
            (network.getValidity().getContinuity() == Validity.ContinuityEnum.FIXED &&
                (DateUtils.isBeforeOrEqual(network.getValidity().getStart(), datePeriod.getStartDateAsOffset()) &&
                    DateUtils.isAfterOrEqual(network.getValidity().getEnd(), datePeriod.getEndDateAsOffset()))));
    }

    private NestedAggregationBuilder aggregateRealisationsByOrganizingOrganisationAndStudyElement(String aggregationName,
                                                                                                  BoolQueryBuilder filter,
                                                                                                  int organisationAmount,
                                                                                                  int pageSize) {
        return AggregationBuilders.nested(aggregationName, "realisations")
            .subAggregation(AggregationBuilders.filter("realisationsFilter", filter)
                .subAggregation(AggregationBuilders.reverseNested("byOrganizingOrganisationId")
                    .subAggregation(AggregationBuilders.terms("organizingOrganisationId").field("organizingOrganisationId").size(organisationAmount)
                        .subAggregation(AggregationBuilders.terms("studyElementId").field("studyElementId").size(pageSize)))));
    }

    private NestedAggregationBuilder aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(String aggregationName,
                                                                                                                BoolQueryBuilder filter,
                                                                                                                int organisationAmount,
                                                                                                                int pageSize) {
        return AggregationBuilders.nested(aggregationName, "completionOptions")
            .subAggregation(AggregationBuilders.nested("assessmentItems", "completionOptions.assessmentItems")
                .subAggregation(AggregationBuilders.nested("realisations", "completionOptions.assessmentItems.realisations")
                    .subAggregation(AggregationBuilders.filter("realisationsFilter", filter)
                        .subAggregation(AggregationBuilders.reverseNested("byOrganizingOrganisationId")
                            .subAggregation(AggregationBuilders.terms("organizingOrganisationId").field("organizingOrganisationId").size(organisationAmount)
                                .subAggregation(AggregationBuilders.terms("studyElementId").field("studyElementId").size(pageSize)))))));
    }

    private AbstractAggregationBuilder aggregateRealisationTeachingLanguages(String aggregationName, RealisationTeachingLanguageFilters filters,
                                                                             String[] networkIds, int pageSize) {
        NestedQueryBuilder networkQuery = getRealisationNetworkQuery("realisations", networkIds);

        if(filters != null) {
            AbstractAggregationBuilder aggs;

            if(filters.getRealisationFilter() != null) {
                aggs = getRealisationTeachingLanguageAggregateQuery(aggregationName, filters.getCourseUnitFilter(), filters.getRealisationFilter(), pageSize);
            } else {
                aggs = getRealisationTeachingLanguageAggregateQuery(aggregationName, filters.getCourseUnitFilter(), networkQuery, pageSize);
            }

            return aggs;
        }

        return getRealisationTeachingLanguageAggregateQuery(aggregationName, null, networkQuery, pageSize);
    }

    private AbstractAggregationBuilder getRealisationTeachingLanguageAggregateQuery(String aggregationName, QueryBuilder courseUnitFilter,
                                                                                    QueryBuilder realisationFilter, int pageSize) {
        FilterAggregationBuilder body = AggregationBuilders.filter("realisationFilter", realisationFilter)
                .subAggregation(AggregationBuilders.terms("status").field("realisations.status")
                    .subAggregation(AggregationBuilders.terms("teachingLanguages").field("realisations.teachingLanguage.lowercase")
                        .missing(TeachingLanguage.UNSPECIFIED.getValue()).size(400)
                            .subAggregation(AggregationBuilders.reverseNested("studyElements")
                                .subAggregation(AggregationBuilders.terms("id").field("_id").size(pageSize)))));

        if(courseUnitFilter != null) {
            return AggregationBuilders.filter(aggregationName, courseUnitFilter)
                .subAggregation(AggregationBuilders.nested("realisations", "realisations")
                    .subAggregation(body));
        }

        return AggregationBuilders.nested(aggregationName, "realisations").subAggregation(body);
    }

    private AbstractAggregationBuilder aggregateAssessmentItemRealisationTeachingLanguages(String aggregationName,
                                                                                           RealisationTeachingLanguageFilters filters,
                                                                                           String[] networkIds, int pageSize) {
        NestedQueryBuilder networkQuery = getRealisationNetworkQuery("completionOptions.assessmentItems.realisations", networkIds);

        if(filters != null) {
            AbstractAggregationBuilder aggs;

            if(filters.getRealisationFilter() != null) {
                aggs = getAssessmentItemRealisationTeachingLanguageAggregateQuery(aggregationName, filters.getCourseUnitFilter(), filters.getRealisationFilter(), pageSize);
            } else {
                aggs = getAssessmentItemRealisationTeachingLanguageAggregateQuery(aggregationName, filters.getCourseUnitFilter(), networkQuery, pageSize);
            }

            return aggs;
        }

        return getAssessmentItemRealisationTeachingLanguageAggregateQuery(aggregationName, null, networkQuery, pageSize);
    }

    private AbstractAggregationBuilder getAssessmentItemRealisationTeachingLanguageAggregateQuery(String aggregationName, QueryBuilder courseUnitFilter,
                                                                                          QueryBuilder realisationFilter, int pageSize) {
        NestedAggregationBuilder body = AggregationBuilders.nested("assessmentItems", "completionOptions.assessmentItems")
                .subAggregation(AggregationBuilders.nested("realisations", "completionOptions.assessmentItems.realisations")
                    .subAggregation(AggregationBuilders.filter("realisationFilter", realisationFilter)
                        .subAggregation(AggregationBuilders.terms("status").field("completionOptions.assessmentItems.realisations.status")
                            .subAggregation(AggregationBuilders.terms("teachingLanguages")
                                .field("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase")
                                .missing(TeachingLanguage.UNSPECIFIED.getValue()).size(400)
                                    .subAggregation(AggregationBuilders.reverseNested("studyElements")
                                        .subAggregation(AggregationBuilders.terms("id").field("_id").size(pageSize)))))));

        if(courseUnitFilter != null) {
            return AggregationBuilders.filter(aggregationName, courseUnitFilter)
                .subAggregation(AggregationBuilders.nested("completionOptions", "completionOptions")
                    .subAggregation(body));
        }

        return AggregationBuilders.nested(aggregationName, "completionOptions")
            .subAggregation(body);
    }

    private NestedQueryBuilder getRealisationNetworkQuery(String realisationPath, String[] networkIds) {
        return QueryBuilders.nestedQuery(String.format("%s.cooperationNetworks", realisationPath),
            QueryBuilders.termsQuery(String.format("%s.cooperationNetworks.id", realisationPath), networkIds), ScoreMode.None).ignoreUnmapped(true);
    }

    private RealisationTeachingLanguageFilters getRealisationTeachingLanguageFilter(StudiesSearchParameters searchParams,
                                                                  BoolQueryBuilder withTeachingLanguagesQueries,
                                                                  BoolQueryBuilder withoutTeachingLanguageQueries,
                                                                  String realisationPath,
                                                                  String[] networkIds) {

        BoolQueryBuilder courseUnitFilter = null;
        BoolQueryBuilder realisationFilter = null;

        // if no cooperation network limits are given by the actual search, we still need to limit the aggregation by networks here since
        // no realisations outside of current organisation networks are ever returned (see StudyRepositoryImpl#hasSeameNetwork())
        NestedQueryBuilder networkQuery = getRealisationNetworkQuery(realisationPath, networkIds);

        if(!searchParams.isOnlyEnrollable()) {
            if(withTeachingLanguagesQueries != null) {
                withTeachingLanguagesQueries.must(networkQuery);
            }
            if(withoutTeachingLanguageQueries != null) {
                withoutTeachingLanguageQueries.must(networkQuery);
            }
        }

        if(!CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            if(searchParams.getTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue()) &&
                searchParams.getRealisationTeachingLanguages().contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                courseUnitFilter = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termsQuery("teachingLanguage.lowercase", searchParams.getTeachingLanguages()));

                realisationFilter = QueryBuilders.boolQuery()
                    .must(withTeachingLanguagesQueries);
            } else {
                courseUnitFilter = QueryBuilders.boolQuery()
                    .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.termsQuery("teachingLanguage.lowercase", searchParams.getTeachingLanguages()))
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("teachingLanguage.lowercase", searchParams.getTeachingLanguages()))));

                realisationFilter = QueryBuilders.boolQuery()
                    .must(QueryBuilders.boolQuery()
                        .should(withTeachingLanguagesQueries)
                        .should(withoutTeachingLanguageQueries));
            }
        } else if(CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            realisationFilter = QueryBuilders.boolQuery()
                .must(withTeachingLanguagesQueries);
        } else if(withoutTeachingLanguageQueries != null) {
            realisationFilter = QueryBuilders.boolQuery()
                .must(withoutTeachingLanguageQueries);
        }

        if(courseUnitFilter == null && realisationFilter == null) {
            return null;
        }

        return new RealisationTeachingLanguageFilters(courseUnitFilter, realisationFilter);
    }

    private BoolQueryBuilder filterByRealisationEnrollmentNotClosed(String path, String[] allowedStatuses, String[] networkIds) {
        OffsetDateTime now = OffsetDateTime.now();

        BoolQueryBuilder realisations = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(String.format("%s.enrollmentClosed", path), false))
            .must(QueryBuilders.termsQuery(String.format("%s.status", path), allowedStatuses))
            .must(getEnrollableDuringDateQuery(String.format("%s.enrollmentStartDateTime", path), String.format("%s.enrollmentEndDateTime", path), now))
            .must(filterByCooperationNetworks(path, networkIds, "now/d", "now/d"));
        
        return realisations;
    }

    private BoolQueryBuilder filterByRealisationEnrollmentStartDateTime(String path, SemesterDatePeriod semesterPeriod, String[] allowedStatuses,
                                                                        String[] networkIds) {
        BoolQueryBuilder realisations = QueryBuilders.boolQuery()
            .must(getEnrolmentPeriodDateTimeQuery(String.format("%s.enrollmentStartDateTime", path), semesterPeriod))
            .must(QueryBuilders.termsQuery(String.format("%s.status", path), allowedStatuses))
            .must(filterByCooperationNetworks(path, networkIds,
                semesterPeriod.getStartDateAsOffset().format(DateUtils.getFormatter()),
                semesterPeriod.getEndDateAsOffset().format(DateUtils.getFormatter())));

        return realisations;
    }

    private NestedQueryBuilder filterByCooperationNetworks(String path, String[] networkIds, String validityStartDateTime, String validityEndDateTime) {
        return QueryBuilders.nestedQuery(String.format("%s.cooperationNetworks", path),
            QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery(String.format("%s.cooperationNetworks.id", path), networkIds))
                .must(QueryBuilders.boolQuery()
                    .should(QueryBuilders.boolQuery()
                        .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.cooperationNetworks.validityStartDate", path))))
                        .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.cooperationNetworks.validityEndDate", path)))))
                    .should(QueryBuilders.boolQuery()
                        .must(QueryBuilders.rangeQuery(String.format("%s.cooperationNetworks.validityStartDate", path))
                            .lte(validityStartDateTime))
                        .must(QueryBuilders.boolQuery()
                            .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(String.format("%s.cooperationNetworks.validityEndDate", path))))
                            .should(QueryBuilders.rangeQuery(String.format("%s.cooperationNetworks.validityEndDate", path))
                                .gte(validityEndDateTime))))),
            ScoreMode.None).ignoreUnmapped(true);
    }

    private BoolQueryBuilder getEnrolmentPeriodDateTimeQuery(String startField, SemesterDatePeriod semesterPeriod) {
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery(startField).gte(semesterPeriod.getStartDateAsOffset().format(DateUtils.getFormatter())))
            .must(QueryBuilders.rangeQuery(startField).lte(semesterPeriod.getEndDateAsOffset().format(DateUtils.getFormatter())));
    }

    private BoolQueryBuilder getEnrollableDuringDateQuery(String startField, String endField, OffsetDateTime date) {
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery(startField).lte(date.format(DateUtils.getFormatter())))
            .must(QueryBuilders.rangeQuery(endField).gte(date.format(DateUtils.getFormatter())));
    }

    private class RealisationTeachingLanguageFilters {
        private BoolQueryBuilder courseUnitFilter;
        private BoolQueryBuilder realisationFilter;

        public RealisationTeachingLanguageFilters(BoolQueryBuilder courseUnitFilter, BoolQueryBuilder realisationFilter) {
            this.courseUnitFilter = courseUnitFilter;
            this.realisationFilter = realisationFilter;
        }

        public BoolQueryBuilder getCourseUnitFilter() {
            return courseUnitFilter;
        }

        public void setCourseUnitFilter(BoolQueryBuilder courseUnitFilter) {
            this.courseUnitFilter = courseUnitFilter;
        }

        public BoolQueryBuilder getRealisationFilter() {
            return realisationFilter;
        }

        public void setRealisationFilter(BoolQueryBuilder realisationFilter) {
            this.realisationFilter = realisationFilter;
        }
    }
}
