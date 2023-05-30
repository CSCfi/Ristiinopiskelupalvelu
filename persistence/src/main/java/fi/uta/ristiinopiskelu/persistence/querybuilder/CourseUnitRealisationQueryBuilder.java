package fi.uta.ristiinopiskelu.persistence.querybuilder;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

public class CourseUnitRealisationQueryBuilder extends AbstractStudiesQueryBuilder {

    private Logger logger = LoggerFactory.getLogger(CourseUnitRealisationQueryBuilder.class);

    public void filterByRealisationStartAndEndDate(BoolQueryBuilder mainRealisationQuery, BoolQueryBuilder mainAssessmentItemQuery,
                                                   LocalDate startDate, LocalDate endDate) {
        if(startDate == null && endDate == null) {
            return;
        }

        BoolQueryBuilder realisationDateQuery = QueryBuilders.boolQuery();
        BoolQueryBuilder assessmentItemRealisationDateQuery = QueryBuilders.boolQuery();

        if(startDate != null) {
            realisationDateQuery.must(QueryBuilders.rangeQuery("realisations.startDate").lte(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            assessmentItemRealisationDateQuery.must(QueryBuilders.rangeQuery("completionOptions.assessmentItems.realisations.startDate")
                .lte(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }

        if(endDate != null) {
            realisationDateQuery.must(QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("realisations.endDate")))
                .should(QueryBuilders.rangeQuery("realisations.endDate")
                    .gte(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))));

            assessmentItemRealisationDateQuery.must(QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("completionOptions.assessmentItems.realisations.endDate")))
                .should(QueryBuilders.rangeQuery("completionOptions.assessmentItems.realisations.endDate")
                    .gte(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))));
        }

        mainRealisationQuery.must(realisationDateQuery);
        mainAssessmentItemQuery.must(assessmentItemRealisationDateQuery);
    }

    public void filterByRealisationEnrollmentStartAndEndDateTime(BoolQueryBuilder mainRealisationQuery,
                                                                 BoolQueryBuilder mainAssessmentItemQuery,
                                                                 OffsetDateTime enrollmentStartDateTimeFrom,
                                                                 OffsetDateTime enrollmentStartDateTimeTo,
                                                                 OffsetDateTime enrollmentEndDateTimeFrom,
                                                                 OffsetDateTime enrollmentEndDateTimeTo) {

        if(enrollmentStartDateTimeFrom == null && enrollmentStartDateTimeTo == null && enrollmentEndDateTimeFrom == null && enrollmentEndDateTimeTo == null) {
            return;
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        BoolQueryBuilder realisationDateQuery = QueryBuilders.boolQuery();
        BoolQueryBuilder assessmentItemRealisationDateQuery = QueryBuilders.boolQuery();

        if(enrollmentStartDateTimeFrom != null || enrollmentStartDateTimeTo != null) {
            RangeQueryBuilder rqb = QueryBuilders.rangeQuery("realisations.enrollmentStartDateTime");
            RangeQueryBuilder aqb = QueryBuilders.rangeQuery("completionOptions.assessmentItems.realisations.enrollmentStartDateTime");

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

            realisationDateQuery.must(rqb);
            assessmentItemRealisationDateQuery.must(aqb);
        }

        if(enrollmentEndDateTimeFrom != null || enrollmentEndDateTimeTo != null) {
            RangeQueryBuilder rqb = QueryBuilders.rangeQuery("realisations.enrollmentEndDateTime");
            RangeQueryBuilder aqb = QueryBuilders.rangeQuery("completionOptions.assessmentItems.realisations.enrollmentEndDateTime");

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
                realisationDateQuery.must(QueryBuilders.boolQuery()
                    .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("realisations.enrollmentEndDateTime")))
                    .should(rqb));
                assessmentItemRealisationDateQuery.must(QueryBuilders.boolQuery()
                    .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("completionOptions.assessmentItems.realisations.enrollmentEndDateTime")))
                    .should(aqb));
            } else {
                realisationDateQuery.must(rqb);
                assessmentItemRealisationDateQuery.must(aqb);
            }
        }

        mainAssessmentItemQuery.must(assessmentItemRealisationDateQuery);
        mainRealisationQuery.must(realisationDateQuery);
    }

    public void filterByRealisationNameOrIdentifierCode(BoolQueryBuilder mainRealisationQuery, BoolQueryBuilder mainAssessmentItemQuery,
                                                        String queryString, Language lang) {
        if(StringUtils.isEmpty(queryString) || lang == null) {
            return;
        }

        String formattedQuery = String.format("*%s*", queryString.toLowerCase());

        BoolQueryBuilder realisationNameOrCodeQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                .should(wildcardQuery(String.format("realisations.name.values.%s.lowercase", lang.getValue()), formattedQuery))
                .should(wildcardQuery("realisations.realisationIdentifierCode.lowercase", formattedQuery)));

        BoolQueryBuilder assessmentItemRealisationNameOrCodeQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                .should(wildcardQuery(String.format("completionOptions.assessmentItems.realisations.name.values.%s.lowercase", lang.getValue()), formattedQuery))
                .should(wildcardQuery("completionOptions.assessmentItems.realisations.realisationIdentifierCode.lowercase", formattedQuery)));

        mainAssessmentItemQuery.must(assessmentItemRealisationNameOrCodeQuery);
        mainRealisationQuery.must(realisationNameOrCodeQuery);
    }

    public void filterByRealisationCooperationNetwork(BoolQueryBuilder mainRealisationQuery, BoolQueryBuilder mainAssessmentItemQuery,
                                                      List<String> networkIds, boolean onlyEnrollable) {

        if(!onlyEnrollable) {
            return;
        }

        BoolQueryBuilder networkQuery = getNetworksValidFilter(
            "realisations.cooperationNetworks", networkIds, false);
        BoolQueryBuilder assessmentItemRealisatioNetworkQuery = getNetworksValidFilter(
            "completionOptions.assessmentItems.realisations.cooperationNetworks", networkIds, false);

        NestedQueryBuilder nestedNetworkQuery = QueryBuilders.nestedQuery(
            "realisations.cooperationNetworks", networkQuery, ScoreMode.None).ignoreUnmapped(true);
        NestedQueryBuilder nestedAssessmentItemRealisationNetworkQuery = QueryBuilders.nestedQuery(
            "completionOptions.assessmentItems.realisations.cooperationNetworks",
            assessmentItemRealisatioNetworkQuery, ScoreMode.None).ignoreUnmapped(true)
            .innerHit(new InnerHitBuilder("assessmentItemRealisationNetworksValidQuery_cooperationNetworks"));

        // check enrollmentClosed
        BoolQueryBuilder realisationEnrollmentNotClosedQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("realisations.enrollmentClosed", false));
        BoolQueryBuilder assessmentItemRealisationEnrollmentNotClosedQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("completionOptions.assessmentItems.realisations.enrollmentClosed", false));

        mainRealisationQuery.must(QueryBuilders.boolQuery()
            .must(nestedNetworkQuery)
            .must(realisationEnrollmentNotClosedQuery));

        mainAssessmentItemQuery.must(QueryBuilders.boolQuery()
            .must(nestedAssessmentItemRealisationNetworkQuery)
            .must(assessmentItemRealisationEnrollmentNotClosedQuery));
    }

    public void filterByTeachingLanguages(BoolQueryBuilder mainRealisationQuery, BoolQueryBuilder mainAssessmentItemQuery,
                                          List<String> realisationTeachingLanguages) {
        if(!CollectionUtils.isEmpty(realisationTeachingLanguages)) {
            BoolQueryBuilder realisationTeachingLanguageQuery = QueryBuilders.boolQuery();
            BoolQueryBuilder assessmentItemRealisationTeachingLanguageQuery = QueryBuilders.boolQuery();

            String[] realisationTeachingLanguageValuesExcludingUnspecified = realisationTeachingLanguages.stream()
                .filter(tl -> !tl.equals(TeachingLanguage.UNSPECIFIED.getValue()))
                .toArray(String[]::new);

            if (realisationTeachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                BoolQueryBuilder realisationTeachingLanguageUnspecifiedQuery =
                    QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("realisations.teachingLanguage.lowercase"));

                BoolQueryBuilder assesmentItemTeachingLanguageUnspecifiedQuery =
                    QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase"));

                if(realisationTeachingLanguageValuesExcludingUnspecified != null && realisationTeachingLanguageValuesExcludingUnspecified.length > 0) {
                    realisationTeachingLanguageQuery.should(realisationTeachingLanguageUnspecifiedQuery);
                    assessmentItemRealisationTeachingLanguageQuery.should(assesmentItemTeachingLanguageUnspecifiedQuery);
                } else {
                    realisationTeachingLanguageQuery.must(realisationTeachingLanguageUnspecifiedQuery);
                    assessmentItemRealisationTeachingLanguageQuery.must(assesmentItemTeachingLanguageUnspecifiedQuery);
                }
            }

            if(realisationTeachingLanguageValuesExcludingUnspecified != null && realisationTeachingLanguageValuesExcludingUnspecified.length > 0) {
                TermsQueryBuilder actualRealisationTeachingLanguageQuery =
                    QueryBuilders.termsQuery("realisations.teachingLanguage.lowercase",
                        realisationTeachingLanguageValuesExcludingUnspecified);

                TermsQueryBuilder actualAssessmentItemTeachingLanguageQuery =
                    QueryBuilders.termsQuery("completionOptions.assessmentItems.realisations.teachingLanguage.lowercase",
                        realisationTeachingLanguageValuesExcludingUnspecified);

                if(realisationTeachingLanguages.contains(TeachingLanguage.UNSPECIFIED.getValue())) {
                    realisationTeachingLanguageQuery.should(actualRealisationTeachingLanguageQuery);
                    assessmentItemRealisationTeachingLanguageQuery.should(actualAssessmentItemTeachingLanguageQuery);
                } else {
                    realisationTeachingLanguageQuery.must(actualRealisationTeachingLanguageQuery);
                    assessmentItemRealisationTeachingLanguageQuery.must(actualAssessmentItemTeachingLanguageQuery);
                }
            }

            mainRealisationQuery.must(realisationTeachingLanguageQuery);
            mainAssessmentItemQuery.must(assessmentItemRealisationTeachingLanguageQuery);
        }
    }

    public void filterByStatuses(BoolQueryBuilder mainRealisationQuery, BoolQueryBuilder mainAssessmentItemQuery, List<StudyStatus> statuses) {
        if(CollectionUtils.isEmpty(statuses)) {
            return;
        }

        mainRealisationQuery.must(QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("realisations.status",
                statuses.stream().map(s -> s.name()).collect(Collectors.toList()))));

        mainAssessmentItemQuery.must(QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("completionOptions.assessmentItems.realisations.status",
                statuses.stream().map(s -> s.name()).collect(Collectors.toList()))));
    }
    
    public NestedQueryBuilder generateAssessmentItemRealisationQuery(QueryBuilder innerQuery, String innerHitsName) {
        return QueryBuilders.nestedQuery("completionOptions",
            QueryBuilders.nestedQuery("completionOptions.assessmentItems",
                QueryBuilders.nestedQuery("completionOptions.assessmentItems.realisations", innerQuery,
                    ScoreMode.None).ignoreUnmapped(true).innerHit(new InnerHitBuilder(StringUtils.hasText(innerHitsName) ? innerHitsName + "_realisations" : null)),
                ScoreMode.None).ignoreUnmapped(true).innerHit(new InnerHitBuilder(StringUtils.hasText(innerHitsName) ? innerHitsName + "_assessmentItems" : null)),
            ScoreMode.None).ignoreUnmapped(true).innerHit(new InnerHitBuilder(StringUtils.hasText(innerHitsName) ? innerHitsName : null));
    }

    public NestedQueryBuilder generateRealisationQuery(QueryBuilder innerQuery, String innerHitsName) {
        return QueryBuilders.nestedQuery("realisations", innerQuery, ScoreMode.None).ignoreUnmapped(true)
            .innerHit(new InnerHitBuilder(StringUtils.hasText(innerHitsName) ? innerHitsName : null));
    }
}
