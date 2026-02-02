package fi.uta.ristiinopiskelu.persistence.querybuilder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CourseUnitRealisationQueryBuilder extends AbstractStudiesQueryBuilder {

    private Logger logger = LoggerFactory.getLogger(CourseUnitRealisationQueryBuilder.class);

    public void filterByRealisationStartAndEndDate(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery,
                                                   LocalDate startDate, LocalDate endDate) {
        if(startDate == null && endDate == null) {
            return;
        }

        BoolQuery.Builder realisationDateQuery = new BoolQuery.Builder();
        BoolQuery.Builder assessmentItemRealisationDateQuery = new BoolQuery.Builder();

        if(startDate != null) {
            realisationDateQuery.must(q -> q
                .range(rq -> rq.date(rq2 -> rq2
                    .field("realisations.startDate")
                    .lte(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))));

            assessmentItemRealisationDateQuery.must(q -> q
                .range(rq -> rq.date(rq2 -> rq2
                    .field("completionOptions.assessmentItems.realisations.startDate")
                    .lte(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))));
        }

        if(endDate != null) {
            realisationDateQuery.must(q -> q
                .bool(bq -> bq
                    .should(q2 -> q2
                        .bool(bq2 -> bq2
                            .mustNot(eq -> eq
                                .exists(eq2 -> eq2.field("realisations.endDate")))))
                    .should(q2 -> q2
                        .range(rq -> rq.date(rq2 -> rq2
                            .field("realisations.endDate")
                            .gte(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))))));

            assessmentItemRealisationDateQuery.must(q -> q.bool(bq -> bq
                .should(q2 -> q2.bool(bq2 -> bq2.mustNot(eq -> eq.exists(eq2 -> eq2.field("completionOptions.assessmentItems.realisations.endDate")))))
                .should(q2 -> q2.range(rq -> rq.date(rq2 -> rq2
                    .field("completionOptions.assessmentItems.realisations.endDate")
                    .gte(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))))));
        }

        mainRealisationQuery.must(realisationDateQuery.build()._toQuery());
        mainAssessmentItemQuery.must(assessmentItemRealisationDateQuery.build()._toQuery());
    }

    public void filterByRealisationEnrollmentStartAndEndDateTime(BoolQuery.Builder mainRealisationQuery,
                                                                 BoolQuery.Builder mainAssessmentItemQuery,
                                                                 OffsetDateTime enrollmentStartDateTimeFrom,
                                                                 OffsetDateTime enrollmentStartDateTimeTo,
                                                                 OffsetDateTime enrollmentEndDateTimeFrom,
                                                                 OffsetDateTime enrollmentEndDateTimeTo) {

        if(enrollmentStartDateTimeFrom == null && enrollmentStartDateTimeTo == null && enrollmentEndDateTimeFrom == null && enrollmentEndDateTimeTo == null) {
            return;
        }

        BoolQuery.Builder realisationDateQuery = new BoolQuery.Builder();
        BoolQuery.Builder assessmentItemRealisationDateQuery = new BoolQuery.Builder();

        if(enrollmentStartDateTimeFrom != null || enrollmentStartDateTimeTo != null) {
            DateRangeQuery.Builder rqb = new DateRangeQuery.Builder().field("realisations.enrollmentStartDateTime");
            DateRangeQuery.Builder aqb = new DateRangeQuery.Builder().field("completionOptions.assessmentItems.realisations.enrollmentStartDateTime");

            if (enrollmentStartDateTimeFrom != null) {
                String startFrom = enrollmentStartDateTimeFrom.format(DateUtils.getFormatter());
                rqb.gte(startFrom);
                aqb.gte(startFrom);
            }

            if (enrollmentStartDateTimeTo != null) {
                String startTo = enrollmentStartDateTimeTo.format(DateUtils.getFormatter());
                rqb.lte(startTo);
                aqb.lte(startTo);
            }

            realisationDateQuery.must(rqb.build()._toRangeQuery()._toQuery());
            assessmentItemRealisationDateQuery.must(aqb.build()._toRangeQuery()._toQuery());
        }

        if(enrollmentEndDateTimeFrom != null || enrollmentEndDateTimeTo != null) {
            DateRangeQuery.Builder rqb = new DateRangeQuery.Builder().field("realisations.enrollmentEndDateTime");
            DateRangeQuery.Builder aqb = new DateRangeQuery.Builder().field("completionOptions.assessmentItems.realisations.enrollmentEndDateTime");

            if (enrollmentEndDateTimeFrom != null) {
                String endFrom = enrollmentEndDateTimeFrom.format(DateUtils.getFormatter());
                rqb.gte(endFrom);
                aqb.gte(endFrom);
            }

            if (enrollmentEndDateTimeTo != null) {
                String endTo = enrollmentEndDateTimeTo.format(DateUtils.getFormatter());
                rqb.lte(endTo);
                aqb.lte(endTo);
            }

            if (enrollmentEndDateTimeTo == null) {
                realisationDateQuery.must(q -> q.bool(bq -> bq
                    .should(bq2 -> bq2
                        .bool(bq3 -> bq3
                            .mustNot(eq -> eq
                                .exists(eq2 -> eq2
                                    .field("realisations.enrollmentEndDateTime")))))
                    .should(rqb.build()._toRangeQuery()._toQuery())));
                assessmentItemRealisationDateQuery.must(q -> q.bool(bq -> bq
                    .should(q2 -> q2
                        .bool(bq2 -> bq2
                            .mustNot(eq -> eq
                                .exists(eq2 -> eq2
                                    .field("completionOptions.assessmentItems.realisations.enrollmentEndDateTime")))))
                    .should(aqb.build()._toRangeQuery()._toQuery())));
            } else {
                realisationDateQuery.must(rqb.build()._toRangeQuery()._toQuery());
                assessmentItemRealisationDateQuery.must(aqb.build()._toRangeQuery()._toQuery());
            }
        }

        mainAssessmentItemQuery.must(assessmentItemRealisationDateQuery.build()._toQuery());
        mainRealisationQuery.must(realisationDateQuery.build()._toQuery());
    }

    public void filterByRealisationNameOrIdentifierCode(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery,
                                                        String queryString, @Nullable Language lang) {
        if(StringUtils.isEmpty(queryString)) {
            return;
        }

        // If lang is not given then target all valid name languages (fi, en, sv)
        Language[] languages = lang != null ? new Language[] { lang } : Language.values();

        String formattedQuery = String.format("*%s*", queryString.toLowerCase());

        Query realisationNameOrCodeQuery = new Query.Builder().bool(q -> q
            .must(q2 -> q2.bool(bq -> {
                for (Language l: languages) {
                    bq.should(q3 -> q3.
                        wildcard(wq -> wq
                            .field(String.format("realisations.name.values.%s.lowercase", l.getValue()))
                            .value(formattedQuery)));
                }
                bq.should(q3 -> q3
                    .wildcard(wq -> wq
                        .field("realisations.realisationIdentifierCode.lowercase")
                        .value(formattedQuery)));
                return bq;
            })))
            .build();

        Query assessmentItemRealisationNameOrCodeQuery = new Query.Builder().bool(q -> q
            .must(q2 -> q2.bool(bq -> {
                for (Language l: languages) {
                    bq.should(q3 -> q3
                        .wildcard(wq -> wq
                            .field(String.format("completionOptions.assessmentItems.realisations.name.values.%s.lowercase", l.getValue()))
                            .value(formattedQuery)));
                }
                bq.should(q3 -> q3
                    .wildcard(wq -> wq
                        .field("completionOptions.assessmentItems.realisations.realisationIdentifierCode.lowercase")
                        .value(formattedQuery)));
                return bq;
            })))
            .build();

        mainAssessmentItemQuery.must(assessmentItemRealisationNameOrCodeQuery);
        mainRealisationQuery.must(realisationNameOrCodeQuery);
    }

    public void filterByRealisationCooperationNetwork(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery,
                                                      List<String> networkIds, boolean onlyEnrollable) {

        if(!onlyEnrollable) {
            return;
        }

        Query networkQuery = getNetworksValidFilter(
            "realisations.cooperationNetworks", networkIds, false);
        Query assessmentItemRealisatioNetworkQuery = getNetworksValidFilter(
            "completionOptions.assessmentItems.realisations.cooperationNetworks", networkIds, false);

        Query nestedNetworkQuery = new Query.Builder().nested(q -> q
                .path("realisations.cooperationNetworks")
                .query(networkQuery)
                .scoreMode(ChildScoreMode.None)
                .ignoreUnmapped(true))
            .build();

        Query nestedAssessmentItemRealisationNetworkQuery = new Query.Builder().nested(q -> q
                .path("completionOptions.assessmentItems.realisations.cooperationNetworks")
                .query(assessmentItemRealisatioNetworkQuery)
                .scoreMode(ChildScoreMode.None)
                .ignoreUnmapped(true)
                .innerHits(ih -> ih.name("assessmentItemRealisationNetworksValidQuery_cooperationNetworks")))
            .build();

        // check enrollmentClosed
        Query realisationEnrollmentNotClosedQuery = new Query.Builder().bool(q -> q
                .must(q2 -> q2
                    .term(tq -> tq
                        .field("realisations.enrollmentClosed")
                        .value(false))))
            .build();

        Query assessmentItemRealisationEnrollmentNotClosedQuery = new Query.Builder().bool(q -> q
                .must(q2 -> q2
                    .term(tq -> tq
                        .field("completionOptions.assessmentItems.realisations.enrollmentClosed")
                        .value(false))))
            .build();

        mainRealisationQuery.must(q -> q.bool(bq -> bq
            .must(nestedNetworkQuery)
            .must(realisationEnrollmentNotClosedQuery)));

        mainAssessmentItemQuery.must(q -> q.bool(bq -> bq
            .must(nestedAssessmentItemRealisationNetworkQuery)
            .must(assessmentItemRealisationEnrollmentNotClosedQuery)));
    }

    public void filterByTeachingLanguages(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery,
                                          List<String> realisationTeachingLanguages) {
        if(!CollectionUtils.isEmpty(realisationTeachingLanguages)) {
            BoolQuery.Builder realisationTeachingLanguageQuery = new BoolQuery.Builder();
            BoolQuery.Builder assessmentItemRealisationTeachingLanguageQuery = new BoolQuery.Builder();

            List<FieldValue> realisationTeachingLanguageValuesExcludingUnspecified = realisationTeachingLanguages.stream()
                .filter(tl -> !tl.equals(TeachingLanguage.UNSPECIFIED.getValue()))
                .map(FieldValue::of)
                .toList();

            if (realisationTeachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                Query realisationTeachingLanguageUnspecifiedQuery = new Query.Builder().bool(q -> q
                        .mustNot(q2 -> q2
                            .exists(eq -> eq
                                .field("realisations.teachingLanguage.lowercase"))))
                    .build();

                Query assesmentItemTeachingLanguageUnspecifiedQuery = new Query.Builder().bool(q -> q
                        .mustNot(q2 -> q2
                            .exists(eq -> eq
                                .field("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase"))))
                    .build();

                if(!CollectionUtils.isEmpty(realisationTeachingLanguageValuesExcludingUnspecified)) {
                    realisationTeachingLanguageQuery.should(realisationTeachingLanguageUnspecifiedQuery);
                    assessmentItemRealisationTeachingLanguageQuery.should(assesmentItemTeachingLanguageUnspecifiedQuery);
                } else {
                    realisationTeachingLanguageQuery.must(realisationTeachingLanguageUnspecifiedQuery);
                    assessmentItemRealisationTeachingLanguageQuery.must(assesmentItemTeachingLanguageUnspecifiedQuery);
                }
            }

            if(!CollectionUtils.isEmpty(realisationTeachingLanguageValuesExcludingUnspecified)) {
                Query actualRealisationTeachingLanguageQuery = new Query.Builder().terms(tq -> tq
                        .field("realisations.teachingLanguage.lowercase")
                        .terms(tqf -> tqf.value(realisationTeachingLanguageValuesExcludingUnspecified)))
                    .build();

                Query actualAssessmentItemTeachingLanguageQuery = new Query.Builder().terms(tq -> tq
                        .field("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase")
                        .terms(tqf -> tqf.value(realisationTeachingLanguageValuesExcludingUnspecified)))
                    .build();

                if(realisationTeachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                    realisationTeachingLanguageQuery.should(actualRealisationTeachingLanguageQuery);
                    assessmentItemRealisationTeachingLanguageQuery.should(actualAssessmentItemTeachingLanguageQuery);
                } else {
                    realisationTeachingLanguageQuery.must(actualRealisationTeachingLanguageQuery);
                    assessmentItemRealisationTeachingLanguageQuery.must(actualAssessmentItemTeachingLanguageQuery);
                }
            }

            mainRealisationQuery.must(realisationTeachingLanguageQuery.build()._toQuery());
            mainAssessmentItemQuery.must(assessmentItemRealisationTeachingLanguageQuery.build()._toQuery());
        }
    }

    public void filterByStatuses(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery, List<StudyStatus> statuses) {
        if(CollectionUtils.isEmpty(statuses)) {
            return;
        }

        mainRealisationQuery.must(q -> q
            .terms(tq -> tq
                .field("realisations.status")
                .terms(tqf -> tqf.value(statuses.stream().map(s -> FieldValue.of(s.name())).toList()))));

        mainAssessmentItemQuery.must(q -> q
            .terms(tq -> tq
                .field("completionOptions.assessmentItems.realisations.status")
                .terms(tqf -> tqf.value(statuses.stream().map(s -> FieldValue.of(s.name())).toList()))));
    }
    
    public Query generateAssessmentItemRealisationQuery(Query innerQuery, String innerHitsName) {
        return new Query.Builder().nested(q -> q
                        .path("completionOptions.assessmentItems.realisations")
                        .ignoreUnmapped(true)
                        .innerHits(ih -> ih.name(StringUtils.hasText(innerHitsName) ? innerHitsName : null))
                        .query(innerQuery))
                .build();
    }

    public Query generateRealisationQuery(Query innerQuery, String innerHitsName) {
        return new Query.Builder().nested(q -> q
                .path("realisations")
                .ignoreUnmapped(true)
                .innerHits(ih -> ih.name(StringUtils.hasText(innerHitsName) ? innerHitsName : null))
                .scoreMode(ChildScoreMode.None)
                .query(innerQuery))
            .build();
    }

    public void filterByMinEduGuidanceAreas(BoolQuery.Builder mainRealisationQuery, BoolQuery.Builder mainAssessmentItemQuery,
                                            List<MinEduGuidanceArea> minEduGuidanceAreas) {
        if(CollectionUtils.isEmpty(minEduGuidanceAreas)) {
            return;
        }

        mainRealisationQuery.must(q -> q
            .terms(tq -> tq
                .field("realisations.minEduGuidanceArea")
                .terms(tqf -> tqf.value(minEduGuidanceAreas.stream()
                    .map(m -> FieldValue.of(m.getCode()))
                    .toList()))));

        mainAssessmentItemQuery.must(q -> q
            .terms(tq -> tq
                .field("completionOptions.assessmentItems.realisations.minEduGuidanceArea")
                .terms(tqf -> tqf.value(minEduGuidanceAreas.stream()
                    .map(m -> FieldValue.of(m.getCode()))
                    .toList()))));
    }
}
