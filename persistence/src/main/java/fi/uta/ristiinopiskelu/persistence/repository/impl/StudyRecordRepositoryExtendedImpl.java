package fi.uta.ristiinopiskelu.persistence.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.GradeCode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepositoryExtended;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StudyRecordRepositoryExtendedImpl implements StudyRecordRepositoryExtended {

    @Autowired
    protected ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<StudyRecordEntity> findAllByParams(StudyRecordSearchParameters searchParameters) {

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(StringUtils.hasText(searchParameters.getSendingOrganisation())) {
            query.must(QueryBuilders.termQuery("sendingOrganisation", searchParameters.getSendingOrganisation()));
        }

        if(StringUtils.hasText(searchParameters.getReceivingOrganisation())) {
            query.must(QueryBuilders.termQuery("receivingOrganisation", searchParameters.getReceivingOrganisation()));
        }

        if(searchParameters.getCompletedCreditTargetType() != null) {
            query.must(QueryBuilders.nestedQuery("completedCredits",
                QueryBuilders.termQuery("completedCredits.completedCreditTarget.completedCreditTargetType", searchParameters.getCompletedCreditTargetType().name()),
                ScoreMode.None));
        }

        if(StringUtils.hasText(searchParameters.getCompletedCreditTargetId())) {
            query.must(QueryBuilders.nestedQuery("completedCredits",
                QueryBuilders.termQuery("completedCredits.completedCreditTarget.completedCreditTargetId", searchParameters.getCompletedCreditTargetId()),
                ScoreMode.None));
        }

        if(StringUtils.hasText(searchParameters.getCompletedCreditName())) {
            Language searchLanguage = searchParameters.getCompletedCreditNameLanguage();
            if(searchLanguage == null) {
                searchLanguage = Language.FI;
            }

            String formattedQuery = String.format("*%s*", searchParameters.getCompletedCreditName());
            query.must(QueryBuilders.nestedQuery("completedCredits",
                QueryBuilders.wildcardQuery(String.format("completedCredits.completedCreditName.values.%s.lowercase", searchLanguage.getValue()), formattedQuery),
                ScoreMode.None));
        }

        if(searchParameters.getGradeStatus() != null) {
            if(searchParameters.getGradeStatus() == GradeStatus.APPROVED) {
                BoolQueryBuilder completedCreditsQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.nestedQuery("completedCredits",
                        QueryBuilders.existsQuery("completedCredits.assessment.grade.code"), ScoreMode.None))
                    .must(QueryBuilders.nestedQuery("completedCredits",
                        QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("completedCredits.assessment.grade.code",
                            GradeCode.GRADE_HYL.getCode())), ScoreMode.None));
                query.must(completedCreditsQuery);
            } else if(searchParameters.getGradeStatus() == GradeStatus.REJECTED) {
                BoolQueryBuilder completedCreditsQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.nestedQuery("completedCredits",
                        QueryBuilders.existsQuery("completedCredits.assessment.grade.code"), ScoreMode.None))
                    .must(QueryBuilders.nestedQuery("completedCredits",
                        QueryBuilders.termQuery("completedCredits.assessment.grade.code", GradeCode.GRADE_HYL.getCode()), ScoreMode.None));
                query.must(completedCreditsQuery);
            } else {
                query.mustNot(QueryBuilders.nestedQuery("completedCredits",
                    QueryBuilders.existsQuery("completedCredits.assessment.grade.code"), ScoreMode.None));
            }
        }

        if(searchParameters.getCompletionStartDate() != null) {
            query.must(QueryBuilders.rangeQuery("completionDate").from(searchParameters.getCompletionStartDate()));
        }

        if(searchParameters.getCompletionEndDate() != null) {
            query.must(QueryBuilders.rangeQuery("completionDate").to(searchParameters.getCompletionEndDate()));
        }

        if(searchParameters.getMinEduGuidanceArea() != null) {
            query.must(QueryBuilders.termQuery("minEduGuidanceArea", searchParameters.getMinEduGuidanceArea().getCode()));
        }

        if(StringUtils.hasText(searchParameters.getOrganisationResponsibleForCompletionTkCode())) {
            query.must(QueryBuilders.termsQuery("organisationResponsibleForCompletion.organisationTkCode", searchParameters.getOrganisationResponsibleForCompletionTkCode()));
        }

        NativeSearchQuery builder = new NativeSearchQueryBuilder()
                .withQuery(query)
                .withPageable(searchParameters.getPageRequest())
                .build();

        return elasticsearchTemplate.search(builder, StudyRecordEntity.class).get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    @Override
    public SearchResponse findAmounts(StudyRecordAmountSearchParameters searchParams) {

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(StringUtils.hasText(searchParams.getSendingOrganisation())) {
            query.must(QueryBuilders.termQuery("sendingOrganisation", searchParams.getSendingOrganisation()));
        }

        if(StringUtils.hasText(searchParams.getReceivingOrganisation())) {
            query.must(QueryBuilders.termQuery("receivingOrganisation", searchParams.getReceivingOrganisation()));
        }

        if(searchParams.getCompletionDateStart() != null) {
            query.must(QueryBuilders.nestedQuery("completedCredits",
                QueryBuilders.rangeQuery("completedCredits.completionDate").from(searchParams.getCompletionDateStart()),
                ScoreMode.None));
        }

        if(searchParams.getCompletionDateEnd() != null) {
            query.must(QueryBuilders.nestedQuery("completedCredits",
                QueryBuilders.rangeQuery("completedCredits.completionDate").to(searchParams.getCompletionDateEnd()),
                ScoreMode.None));
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .size(0)
            .query(query);

        AggregationBuilder groupByAggregation = null;

        if(searchParams.getGroupBy() == StudyRecordGrouping.SENDING_ORGANISATION) {
            groupByAggregation = AggregationBuilders.terms("organisation").field("sendingOrganisation");
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.RECEIVING_ORGANISATION) {
            groupByAggregation = AggregationBuilders.terms("organisation").field("receivingOrganisation");
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.DATES) {
            String datePattern = "uuuu-MM-dd";

            DateRangeAggregationBuilder dateRangeAgg = AggregationBuilders.dateRange("dateRanges")
                .field("completedCredits.completionDate")
                .keyed(true)
                .format(datePattern);

            for(StudyRecordGroupingDates dates : searchParams.getGroupByDates()) {
                String formattedStart = DateUtils.getFormatted(datePattern, dates.getStart());
                String formattedEnd = DateUtils.getFormatted(datePattern, dates.getEnd());
                String rangeKey = String.format("%s:%s", formattedStart, formattedEnd);
                dateRangeAgg.addRange(rangeKey, formattedStart, formattedEnd);
            }

            searchSourceBuilder.aggregation(AggregationBuilders.nested("dates", "completedCredits")
                .subAggregation(dateRangeAgg));
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.STUDYELEMENT_IDENTIFIER_CODE) {
            searchSourceBuilder.aggregation(AggregationBuilders.nested("completedCredits", "completedCredits")
                .subAggregation(AggregationBuilders.terms("studyElementIdentifierCodes")
                    .field("completedCredits.completedCreditTarget.completedCreditTargetIdentifierCode")));
        }

        if(groupByAggregation != null) {
            if (searchParams.getDivideBy() == StudyRecordDividing.GRADING) {
                searchSourceBuilder.aggregation(groupByAggregation
                    .subAggregation(AggregationBuilders.nested("approved", "completedCredits")
                        .subAggregation(AggregationBuilders.filter("approvedFilter", QueryBuilders.boolQuery()
                            .must(QueryBuilders.existsQuery("completedCredits.assessment.grade.code"))
                            .must(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("completedCredits.assessment.grade.code", "HYL"))))))
                    .subAggregation(AggregationBuilders.nested("rejected", "completedCredits")
                        .subAggregation(AggregationBuilders.filter("rejectedFilter", QueryBuilders.boolQuery()
                            .must(QueryBuilders.existsQuery("completedCredits.assessment.grade.code"))
                            .must(QueryBuilders.termQuery("completedCredits.assessment.grade.code", "HYL")))))
                    .subAggregation(AggregationBuilders.nested("ungraded", "completedCredits")
                        .subAggregation(AggregationBuilders.filter("ungradedFilter", QueryBuilders.boolQuery()
                            .mustNot(QueryBuilders.existsQuery("completedCredits.assessment.grade.code"))))));
            } else if (searchParams.getDivideBy() == StudyRecordDividing.MIN_EDU_GUIDANCE_AREA) {
                searchSourceBuilder.aggregation(groupByAggregation
                    .subAggregation(AggregationBuilders.nested("minEduGuidanceArea", "completedCredits")
                        .subAggregation(AggregationBuilders.terms("code")
                            .field("completedCredits.minEduGuidanceArea"))));
            } else if (searchParams.getDivideBy() == StudyRecordDividing.ORGANISATION_RESPONSIBLE_FOR_COMPLETION) {
                searchSourceBuilder.aggregation(groupByAggregation
                    .subAggregation(AggregationBuilders.nested("organisationResponsibleForCompletion", "completedCredits")
                        .subAggregation(AggregationBuilders.terms("organisationTkCode")
                            .field("completedCredits.organisationResponsibleForCompletion.organisationTkCode"))));
            }
        }

        SearchRequest searchRequest = new SearchRequest(Arrays.asList("opintosuoritukset").toArray(new String[0]), searchSourceBuilder);

        return this.elasticsearchTemplate.execute(client -> {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            // check if something failed. funny that the query might seem to have been successful even though there might have been shard errors, ain't it? ":D"
            if(response.getFailedShards() > 0) {
                for(ShardSearchFailure failure : response.getShardFailures()) {
                    throw new IllegalStateException("Error while searching study record amounts", failure.getCause());
                }
            }

            return response;
        });
    }
}
