package fi.uta.ristiinopiskelu.persistence.querybuilder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsInclude;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchRealisationQueries;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import fi.uta.ristiinopiskelu.persistence.utils.SemesterDatePeriod;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudiesSearchRequestBuilder {

    public SearchRequest build(List<String> indices, BoolQuery.Builder studiesQueryBuilder,
                               StudiesSearchRealisationQueries realisationQueriesWithTeachingLanguage,
                               StudiesSearchRealisationQueries realisationQueriesWithoutTeachingLanguage,
                               PageRequest paging, StudiesSearchParameters searchParams) {

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .query(studiesQueryBuilder.build()._toQuery())
                .from((int) paging.getOffset())
                .size(paging.getPageSize())
                .index(indices);

        if(paging.getSort().isSorted()) {
            searchRequestBuilder.sort(paging.getSort().stream()
                    .map(sort -> new SortOptions.Builder()
                            .field(f -> f
                                    .field(sort.getProperty())
                                    .order(sort.getDirection().isAscending() ? SortOrder.Asc : SortOrder.Desc))
                            .build())
                    .toList());
        }

        List<String> searchParamNetworks = searchParams.getActualNetworkIdsUsedInSearch();

        searchRequestBuilder.aggregations("teachingLanguages", agg -> agg.terms(ta -> ta
            .field("teachingLanguage.lowercase")
            .missing(TeachingLanguage.UNSPECIFIED.getValue())
            .size(400)));

        BoolQuery.Builder realisationsWithTeachingLanguagesQuery =
            realisationQueriesWithTeachingLanguage == null ? null : realisationQueriesWithTeachingLanguage.getAggregationRealisationQuery();
        BoolQuery.Builder realisationsWithoutTeachingLanguagesQuery =
            realisationQueriesWithoutTeachingLanguage == null ? null : realisationQueriesWithoutTeachingLanguage.getAggregationRealisationQuery();
        BoolQuery.Builder assessmentItemRealisationsWithTeachingLanguagesQuery =
            realisationQueriesWithTeachingLanguage == null ? null : realisationQueriesWithTeachingLanguage.getAggregationAssessmentItemRealisationQuery();
        BoolQuery.Builder assessmentItemRealisationsWithoutTeachingLanguagesQuery =
            realisationQueriesWithoutTeachingLanguage == null ? null : realisationQueriesWithoutTeachingLanguage.getAggregationAssessmentItemRealisationQuery();

        RealisationTeachingLanguageFilters realisationTeachingLanguageFilter = getRealisationTeachingLanguageFilter(searchParams,
            realisationsWithTeachingLanguagesQuery, realisationsWithoutTeachingLanguagesQuery, "realisations", searchParamNetworks);

        RealisationTeachingLanguageFilters assessmentItemRealisationTeachingLanguageFilter = getRealisationTeachingLanguageFilter(searchParams,
            assessmentItemRealisationsWithTeachingLanguagesQuery, assessmentItemRealisationsWithoutTeachingLanguagesQuery,
            "completionOptions.assessmentItems.realisations", searchParamNetworks);

        // use the same realisation queries here to filter the realisation aggregates so that results are in sync with the actual search
        searchRequestBuilder.aggregations("realisationTeachingLanguages", aggregateRealisationTeachingLanguages(realisationTeachingLanguageFilter,
                searchParamNetworks, paging.getPageSize()));

        // here too
        searchRequestBuilder.aggregations("assessmentItemRealisationTeachingLanguages", aggregateAssessmentItemRealisationTeachingLanguages(
            assessmentItemRealisationTeachingLanguageFilter, searchParamNetworks, paging.getPageSize()));

        if(!CollectionUtils.isEmpty(searchParamNetworks)) {
            searchRequestBuilder.aggregations("networks", agg -> agg
                .nested(na -> na
                    .path("cooperationNetworks"))
                .aggregations("id", agg2 -> agg2
                    .terms(ta -> ta
                        .field("cooperationNetworks.id")
                        .size(searchParamNetworks.size())
                        .include(TermsInclude.of(ti -> ti
                            .terms(searchParamNetworks))))));
        }

        searchRequestBuilder.aggregations("organisations", agg -> agg
            .terms(ta -> ta
                .field("organizingOrganisationId")
                .size(searchParams.getOrganisationAmount())));

        searchRequestBuilder.aggregations("types", agg -> agg.terms(ta -> ta
                .field("type")));

        SemesterDatePeriod currentSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 0);
        final SemesterDatePeriod currentSemesterStartingToday = new SemesterDatePeriod(OffsetDateTime.now(), currentSemester.getEndDateAsOffset());

        List<String> currentSemesterValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
                .filter(isValidNetworkDuringDatePeriod(currentSemesterStartingToday))
                .map(NetworkEntity::getId)
                .collect(Collectors.toList());

        final SemesterDatePeriod nextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 1);
        List<String> nextSemesterValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
                .filter(isValidNetworkDuringDatePeriod(nextSemester))
                .map(NetworkEntity::getId)
                .collect(Collectors.toList());

        final SemesterDatePeriod semesterAfterNextSemester = SemesterDatePeriod.getSemesterDatePeriod(LocalDate.now(), 2);
        List<String> semesterAfterNextValidNetworkIds = searchParams.getActualNetworksUsedInSearch().stream()
                .filter(isValidNetworkDuringDatePeriod(semesterAfterNextSemester))
                .map(NetworkEntity::getId)
                .collect(Collectors.toList());

        List<String> allowedRealisationStudyStatuses = List.of(StudyStatus.ACTIVE.name());

        searchRequestBuilder.aggregations("enrollableRealisations", aggregateRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentNotClosed("realisations", allowedRealisationStudyStatuses,
                    searchParamNetworks), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchRequestBuilder.aggregations("enrollableAssessmentItemRealisations", aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentNotClosed("completionOptions.assessmentItems.realisations",
                    allowedRealisationStudyStatuses, searchParamNetworks), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchRequestBuilder.aggregations("realisationsEnrollableThisSemester", aggregateRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("realisations", currentSemesterStartingToday,
                    allowedRealisationStudyStatuses, currentSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchRequestBuilder.aggregations("assessmentItemRealisationsEnrollableThisSemester", aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    currentSemesterStartingToday, allowedRealisationStudyStatuses, currentSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchRequestBuilder.aggregations("realisationsEnrollableNextSemester", aggregateRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("realisations", nextSemester, allowedRealisationStudyStatuses,
                    nextSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchRequestBuilder.aggregations("assessmentItemRealisationsEnrollableNextSemester", aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    nextSemester, allowedRealisationStudyStatuses, nextSemesterValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        searchRequestBuilder.aggregations("realisationsEnrollableAfterNextSemester", aggregateRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("realisations", semesterAfterNextSemester,
                    allowedRealisationStudyStatuses, semesterAfterNextValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));
        searchRequestBuilder.aggregations("assessmentItemRealisationsEnrollableAfterNextSemester", aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(
                this.filterByRealisationEnrollmentStartDateTime("completionOptions.assessmentItems.realisations",
                    semesterAfterNextSemester, allowedRealisationStudyStatuses, semesterAfterNextValidNetworkIds), searchParams.getOrganisationAmount(),
            searchParams.getPageSize()));

        return searchRequestBuilder.build();
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

    private Aggregation aggregateRealisationsByOrganizingOrganisationAndStudyElement(Query filter, int organisationAmount,
                                                                                     int pageSize) {
        return new Aggregation.Builder().nested(n -> n
                .path("realisations"))
                .aggregations("realisationsFilter", realisationsFilter -> realisationsFilter
                        .filter(filter)
                        .aggregations("byOrganizingOrganisationId", byOrganizingOrganisationId -> byOrganizingOrganisationId
                                .reverseNested(rn -> rn)
                                .aggregations("organizingOrganisationId", organizingOrganisationId -> organizingOrganisationId
                                        .terms(ta -> ta
                                                .field("organizingOrganisationId")
                                                .size(organisationAmount))
                                                .aggregations("studyElementId", studyElementId -> studyElementId
                                                        .terms(ta -> ta
                                                                .field("studyElementId")
                                                                .size(pageSize))))))
                .build();
    }

    private Aggregation aggregateAssessmentItemRealisationsByOrganizingOrganisationAndStudyElement(Query filter,
                                                                                                   int organisationAmount,
                                                                                                   int pageSize) {

        return new Aggregation.Builder().nested(n -> n
                .path("completionOptions.assessmentItems.realisations"))
                .aggregations("realisationsFilter", realisationsFilter -> realisationsFilter
                        .filter(filter)
                        .aggregations("byOrganizingOrganisationId", byOrganizingOrganisationId -> byOrganizingOrganisationId
                                .reverseNested(rn -> rn)
                                .aggregations("organizingOrganisationId", organizingOrganisationId -> organizingOrganisationId
                                        .terms(ta -> ta
                                                .field("organizingOrganisationId")
                                                .size(organisationAmount))
                                        .aggregations("studyElementId", studyElementId -> studyElementId
                                                .terms(ta -> ta
                                                        .field("studyElementId")
                                                        .size(pageSize))))))
                .build();
    }

    private Aggregation aggregateRealisationTeachingLanguages(RealisationTeachingLanguageFilters filters,
                                                            List<String> networkIds, int pageSize) {
        Query networkQuery = getRealisationNetworkQuery("realisations", networkIds);

        if(filters != null) {
            Aggregation agg;

            if(filters.getRealisationFilter() != null) {
                agg = getRealisationTeachingLanguageAggregateQuery(filters.getCourseUnitFilter(), filters.getRealisationFilter(), pageSize);
            } else {
                agg = getRealisationTeachingLanguageAggregateQuery(filters.getCourseUnitFilter(), networkQuery, pageSize);
            }

            return agg;
        }

        return getRealisationTeachingLanguageAggregateQuery(null, networkQuery, pageSize);
    }

    private Aggregation getRealisationTeachingLanguageAggregateQuery(Query courseUnitFilter, Query realisationFilter, int pageSize) {
         Aggregation body = new Aggregation.Builder().filter(realisationFilter)
             .aggregations("status", status -> status
                 .terms(terms -> terms.field("realisations.status"))
                 .aggregations("teachingLanguages", teachingLanguages -> teachingLanguages
                     .terms(tl -> tl
                         .field("realisations.teachingLanguage.lowercase")
                         .missing(TeachingLanguage.UNSPECIFIED.getValue())
                         .size(400))
                     .aggregations("studyElements", studyElements -> studyElements
                         .reverseNested(rn -> rn)
                         .aggregations("id", id -> id
                             .terms(t -> t
                                 .field("_id")
                                 .size(pageSize))))))
             .build();

        if(courseUnitFilter != null) {
            return new Aggregation.Builder().filter(courseUnitFilter)
                .aggregations("realisations", realisations -> realisations
                    .nested(n -> n
                        .path("realisations"))
                    .aggregations("realisationFilter", body))
                .build();
        }

        return new Aggregation.Builder().nested(realisations -> realisations
                .path("realisations"))
            .aggregations("realisationFilter", body)
            .build();
    }

    private Aggregation aggregateAssessmentItemRealisationTeachingLanguages(RealisationTeachingLanguageFilters filters,
                                                                            List<String> networkIds, int pageSize) {
        Query networkQuery = getRealisationNetworkQuery("completionOptions.assessmentItems.realisations", networkIds);

        if(filters != null) {
            Aggregation agg;

            if(filters.getRealisationFilter() != null) {
                agg = getAssessmentItemRealisationTeachingLanguageAggregateQuery(filters.getCourseUnitFilter(), filters.getRealisationFilter(), pageSize);
            } else {
                agg = getAssessmentItemRealisationTeachingLanguageAggregateQuery(filters.getCourseUnitFilter(), networkQuery, pageSize);
            }

            return agg;
        }

        return getAssessmentItemRealisationTeachingLanguageAggregateQuery(null, networkQuery, pageSize);
    }

    private Aggregation getAssessmentItemRealisationTeachingLanguageAggregateQuery(Query courseUnitFilter, Query realisationFilter, int pageSize) {
        Aggregation body = new Aggregation.Builder().filter(realisationFilter)
            .aggregations("status", status -> status
                .terms(st -> st
                    .field("completionOptions.assessmentItems.realisations.status"))
                .aggregations("teachingLanguages", teachingLanguages -> teachingLanguages
                    .terms(tlt -> tlt
                        .field("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase")
                        .missing(TeachingLanguage.UNSPECIFIED.getValue()).size(400))
                    .aggregations("studyElements", studyElements -> studyElements
                        .reverseNested(sern -> sern)
                        .aggregations("id", id -> id
                            .terms(idt -> idt
                                .field("_id")
                                .size(pageSize))))))
            .build();

        if(courseUnitFilter != null) {
            return new Aggregation.Builder().filter(courseUnitFilter)
                .aggregations("realisations", realisations -> realisations
                    .nested(nested -> nested
                        .path("completionOptions.assessmentItems.realisations"))
                    .aggregations("realisationFilter", body))
                .build();
        }

        return new Aggregation.Builder().nested(nested -> nested
                .path("completionOptions.assessmentItems.realisations"))
            .aggregations("realisationFilter", body)
            .build();
    }

    private Query getRealisationNetworkQuery(String realisationPath, List<String> networkIds) {
        return new Query.Builder().nested(nq -> nq
                .path(String.format("%s.cooperationNetworks", realisationPath))
                .query(termsBuilder -> termsBuilder
                        .terms(new TermsQuery.Builder()
                                .field(String.format("%s.cooperationNetworks.id", realisationPath))
                                .terms(new TermsQueryField.Builder()
                                        .value(networkIds.stream().map(FieldValue::of).collect(Collectors.toList()))
                                        .build())
                                .build()))
                .scoreMode(ChildScoreMode.None)
                .ignoreUnmapped(true))
                .build();
    }

    private RealisationTeachingLanguageFilters getRealisationTeachingLanguageFilter(StudiesSearchParameters searchParams,
                                                                  BoolQuery.Builder withTeachingLanguagesQueries,
                                                                  BoolQuery.Builder withoutTeachingLanguageQueries,
                                                                  String realisationPath,
                                                                  List<String> networkIds) {         
        Query courseUnitFilter = null;
        Query realisationFilter = null;

        // if no cooperation network limits are given by the actual search, we still need to limit the aggregation by networks here since
        // no realisations outside of current organisation networks are ever returned (see StudyRepositoryImpl#hasSeameNetwork())
       Query networkQuery = getRealisationNetworkQuery(realisationPath, networkIds);

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
                

                courseUnitFilter = new Query.Builder().bool(bq -> bq
                                .must(new Query.Builder().terms(tq -> tq
                                                .field("teachingLanguage.lowercase")
                                                .terms(new TermsQueryField.Builder()
                                                        .value(searchParams.getTeachingLanguages().stream()
                                                                .map(FieldValue::of)
                                                                .collect(Collectors.toList()))
                                                        .build()))
                                        .build()))
                        .build();
                
               realisationFilter = new Query.Builder().bool(bq -> bq
                       .must(withTeachingLanguagesQueries.build()._toQuery())).build();
            } else {
                Query teachingLanguageTerms = new Query.Builder().terms(tq -> tq
                                .field("teachingLanguage.lowercase")
                                .terms(new TermsQueryField.Builder()
                                        .value(searchParams.getTeachingLanguages().stream().map(FieldValue::of).collect(Collectors.toList()))
                                        .build()))
                        .build();

                courseUnitFilter = QueryBuilders.bool(bq -> bq
                        .must(QueryBuilders.bool(bq2 -> bq2
                                .should(teachingLanguageTerms)
                                .should(QueryBuilders.bool(bq3 -> bq3
                                        .mustNot(teachingLanguageTerms))))));

                realisationFilter = QueryBuilders.bool(bq -> bq
                        .must(QueryBuilders.bool(bq2 -> bq2
                                        .should(withTeachingLanguagesQueries.build()._toQuery())
                                        .should(withoutTeachingLanguageQueries.build()._toQuery()))));
            }
        } else if(CollectionUtils.isEmpty(searchParams.getTeachingLanguages()) && !CollectionUtils.isEmpty(searchParams.getRealisationTeachingLanguages())) {
            realisationFilter = QueryBuilders.bool(bq -> bq
                    .must(withTeachingLanguagesQueries.build()._toQuery()));
        } else if(withoutTeachingLanguageQueries != null) {
            realisationFilter = QueryBuilders.bool(bq -> bq
                    .must(withoutTeachingLanguageQueries.build()._toQuery()));
        }

        if(courseUnitFilter == null && realisationFilter == null) {
            return null;
        }

        return new RealisationTeachingLanguageFilters(courseUnitFilter, realisationFilter);
    }

    private Query filterByRealisationEnrollmentNotClosed(String path, List<String> allowedStatuses, List<String> networkIds) {
        OffsetDateTime now = OffsetDateTime.now();

        return new Query.Builder().bool(bq -> bq
                        .must(new Query.Builder().term(tq -> tq
                                        .field(String.format("%s.enrollmentClosed", path))
                                        .value(false))
                                .build())
                        .must(new Query.Builder().terms(tq -> tq
                                        .field(String.format("%s.status", path))
                                        .terms(tqf -> tqf.value(allowedStatuses.stream().map(FieldValue::of).collect(Collectors.toList()))))
                                .build())
                        .must(getEnrollableDuringDateQuery(String.format("%s.enrollmentStartDateTime", path), String.format("%s.enrollmentEndDateTime", path), now))
                        .must(filterByCooperationNetworks(path, networkIds, "now/d", "now/d")))
                .build();
    }

    private Query filterByRealisationEnrollmentStartDateTime(String path, SemesterDatePeriod semesterPeriod,
                                                             List<String> allowedStatuses, List<String> networkIds) {
        return new Query.Builder().bool(bq -> bq
                        .must(getEnrolmentPeriodDateTimeQuery(String.format("%s.enrollmentStartDateTime", path), semesterPeriod))
                        .must(new Query.Builder().terms(tq -> tq
                                        .field(String.format("%s.status", path))
                                        .terms(TermsQueryField.of(tqf -> tqf.value(allowedStatuses.stream().map(FieldValue::of).collect(Collectors.toList())))))
                                .build())
                        .must(filterByCooperationNetworks(path, networkIds, semesterPeriod.getStartDateAsOffset().format(DateUtils.getFormatter()),
                                semesterPeriod.getEndDateAsOffset().format(DateUtils.getFormatter()))))
                .build();
    }

    private Query filterByCooperationNetworks(String path, List<String> networkIds, String validityStartDateTime, String validityEndDateTime) {
        return new Query.Builder().nested(nq -> nq
                .path("%s.cooperationNetworks".formatted(path))
                .query(new Query.Builder().bool(bq -> bq
                    .must(new Query.Builder().terms(tq -> tq
                                    .field("%s.cooperationNetworks.id".formatted(path))
                                    .terms(TermsQueryField.of(tqf -> tqf.value(networkIds.stream().map(FieldValue::of).collect(Collectors.toList())))))
                            .build())
                    .must(new Query.Builder().bool(bq2 -> bq2
                        .should(new Query.Builder().bool(bq3 -> bq3
                            .must(new Query.Builder().bool(bq4 -> bq4
                                    .mustNot(new Query.Builder().exists(eq -> eq
                                                    .field("%s.cooperationNetworks.validityStartDate".formatted(path)))
                                            .build()))
                                    .build())
                            .must(new Query.Builder().bool(bq4 -> bq4
                                    .mustNot(new Query.Builder().exists(eq -> eq
                                                    .field("%s.cooperationNetworks.validityEndDate".formatted(path)))
                                            .build()))
                                    .build()))
                                .build())
                        .should(new Query.Builder().bool(bq3 -> bq3
                            .must(new Query.Builder().range(rq -> rq
                                    .field("%s.cooperationNetworks.validityStartDate".formatted(path))
                                    .lte(JsonData.of(validityStartDateTime)))
                                    .build())
                            .must(new Query.Builder().bool(bq4 -> bq4
                                .should(new Query.Builder().bool(bq5 -> bq5
                                        .mustNot(new Query.Builder().exists(bq6 -> bq6
                                                        .field("%s.cooperationNetworks.validityEndDate".formatted(path)))
                                                .build()))
                                        .build())
                                .should(new Query.Builder().range(rq -> rq
                                                .field("%s.cooperationNetworks.validityEndDate".formatted(path))
                                                .gte(JsonData.of(validityEndDateTime)))
                                        .build()))
                                    .build()))
                                .build()))
                            .build()))
                        .build())
                .scoreMode(ChildScoreMode.None)
                .ignoreUnmapped(true))
                .build();
    }

    private Query getEnrolmentPeriodDateTimeQuery(String startField, SemesterDatePeriod semesterPeriod) {
        return new Query.Builder().bool(bq -> bq
                        .must(new Query.Builder().range(rq -> rq
                                        .field(startField)
                                        .gte(JsonData.of(semesterPeriod.getStartDateAsOffset().format(DateUtils.getFormatter()))))
                                .build())
                        .must(new Query.Builder().range(rq -> rq
                                        .field(startField)
                                        .lte(JsonData.of(semesterPeriod.getEndDateAsOffset().format(DateUtils.getFormatter()))))
                                .build()))
                .build();
    }

    private Query getEnrollableDuringDateQuery(String startField, String endField, OffsetDateTime date) {
        return new Query.Builder().bool(bq -> bq
            .must(new Query.Builder().range(rq -> rq
                            .field(startField)
                            .lte(JsonData.of(date.format(DateUtils.getFormatter()))))
                    .build())
            .must(new Query.Builder().range(rq -> rq
                            .field(endField)
                            .gte(JsonData.of(date.format(DateUtils.getFormatter()))))
                    .build()))
            .build();
    }

    private class RealisationTeachingLanguageFilters {
        private Query courseUnitFilter;
        private Query realisationFilter;

        public RealisationTeachingLanguageFilters(Query courseUnitFilter, Query realisationFilter) {
            this.courseUnitFilter = courseUnitFilter;
            this.realisationFilter = realisationFilter;
        }

        public Query getCourseUnitFilter() {
            return courseUnitFilter;
        }

        public void setCourseUnitFilter(Query courseUnitFilter) {
            this.courseUnitFilter = courseUnitFilter;
        }

        public Query getRealisationFilter() {
            return realisationFilter;
        }

        public void setRealisationFilter(Query realisationFilter) {
            this.realisationFilter = realisationFilter;
        }
    }
}
