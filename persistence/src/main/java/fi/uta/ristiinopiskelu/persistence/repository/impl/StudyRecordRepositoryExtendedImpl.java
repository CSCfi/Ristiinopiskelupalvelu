package fi.uta.ristiinopiskelu.persistence.repository.impl;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.GradeCode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepositoryExtended;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.util.StringUtils;

import java.util.List;

public class StudyRecordRepositoryExtendedImpl implements StudyRecordRepositoryExtended {

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<StudyRecordEntity> findAllByParams(StudyRecordSearchParameters searchParameters) {

        BoolQuery.Builder query = new BoolQuery.Builder();

        if(StringUtils.hasText(searchParameters.getSendingOrganisation())) {
            query.must(new Query.Builder().term(tq -> tq
                .field("sendingOrganisation")
                .value(searchParameters.getSendingOrganisation())).build());
        }

        if(StringUtils.hasText(searchParameters.getReceivingOrganisation())) {
            query.must(new Query.Builder().term(tq -> tq
                .field("receivingOrganisation")
                .value(searchParameters.getReceivingOrganisation())).build());
        }

        if(searchParameters.getCompletedCreditTargetType() != null) {
            query.must(new Query.Builder().nested(nq -> nq
                .path("completedCredits")
                .query(TermQuery.of(tq -> tq
                    .field("completedCredits.completedCreditTarget.completedCreditTargetType")
                    .value(searchParameters.getCompletedCreditTargetType().name()))._toQuery())
                .scoreMode(ChildScoreMode.None)).build());
        }

        if(StringUtils.hasText(searchParameters.getCompletedCreditTargetId())) {
            query.must(new Query.Builder().nested(nq -> nq
                .path("completedCredits")
                .query(TermQuery.of(tq -> tq
                    .field("completedCredits.completedCreditTarget.completedCreditTargetId")
                    .value(searchParameters.getCompletedCreditTargetId()))._toQuery())
                .scoreMode(ChildScoreMode.None)).build());
        }

        if(StringUtils.hasText(searchParameters.getCompletedCreditName())) {
            Language searchLanguage = searchParameters.getCompletedCreditNameLanguage();
            if(searchLanguage == null) {
                searchLanguage = Language.FI;
            }

            String formattedQuery = String.format("*%s*", searchParameters.getCompletedCreditName());
            final Language finalSearchLanguage = searchLanguage;

            query.must(new Query.Builder().nested(nq -> nq
                .path("completedCredits")
                .query(WildcardQuery.of(wq -> wq
                    .field(String.format("completedCredits.completedCreditName.values.%s.lowercase", finalSearchLanguage.getValue()))
                    .value(formattedQuery))._toQuery())
                .scoreMode(ChildScoreMode.None)).build());
        }

        if(searchParameters.getGradeStatus() != null) {
            if(searchParameters.getGradeStatus() == GradeStatus.APPROVED) {
                query.must(m -> new Query.Builder().bool(bq -> bq
                    .must(nested -> new Query.Builder().nested(nq -> nq
                        .path("completedCredits")
                        .query(q -> new Query.Builder().exists(eq -> eq
                            .field("completedCredits.assessment.grade.code")))
                        .scoreMode(ChildScoreMode.None)))
                    .must(nested -> new Query.Builder().nested(nq -> nq
                        .path("completedCredits")
                        .query(q -> new Query.Builder().bool(bq2 -> bq2
                            .mustNot(mn -> new Query.Builder().term(tq -> tq
                                .field("completedCredits.assessment.grade.code")
                                .value(GradeCode.GRADE_HYL.getCode())))))
                        .scoreMode(ChildScoreMode.None)))));
            } else if(searchParameters.getGradeStatus() == GradeStatus.REJECTED) {
                query.must(m -> new Query.Builder().bool(bq -> bq
                    .must(nested -> new Query.Builder().nested(nq -> nq
                        .path("completedCredits")
                        .query(e -> new Query.Builder().exists(eq -> eq
                            .field("completedCredits.assessment.grade.code")))
                        .scoreMode(ChildScoreMode.None)))
                    .must(nested -> new Query.Builder().nested(nq -> nq
                        .path("completedCredits")
                        .query(q -> new Query.Builder().term(tq -> tq.field("completedCredits.assessment.grade.code")
                            .value(GradeCode.GRADE_HYL.getCode())))
                        .scoreMode(ChildScoreMode.None)))));                 
            } else {
                query.mustNot(mn -> new Query.Builder().nested(nq -> nq
                        .path("completedCredits")
                        .query(q -> new Query.Builder().exists(eq -> eq
                            .field("completedCredits.assessment.grade.code")))
                        .scoreMode(ChildScoreMode.None)));
            }
        }

        if(searchParameters.getCompletionStartDate() != null) {
            query.must(m -> new Query.Builder().range(rq -> rq
                .field("completionDate")
                .from(DateUtils.getFormatted(searchParameters.getCompletionStartDate()))));
        }

        if(searchParameters.getCompletionEndDate() != null) {
            query.must(m -> new Query.Builder().range(rq -> rq
                .field("completionDate")
                .to(DateUtils.getFormatted(searchParameters.getCompletionEndDate()))));
        }

        if(searchParameters.getMinEduGuidanceArea() != null) {
            query.must(m -> new Query.Builder().term(tq -> tq
                .field("minEduGuidanceArea")
                .value(searchParameters.getMinEduGuidanceArea().getCode())));
        }

        if(StringUtils.hasText(searchParameters.getOrganisationResponsibleForCompletionTkCode())) {
            query.must(m -> new Query.Builder().term(tq -> tq
                .field("organisationResponsibleForCompletion.organisationTkCode")
                .value(searchParameters.getOrganisationResponsibleForCompletionTkCode())));
        }

        NativeQuery nativeQuery = new NativeQueryBuilder()
            .withQuery(query.build()._toQuery())
            .withPageable(searchParameters.getPageRequest())
            .build();

        return elasticsearchTemplate.search(nativeQuery, StudyRecordEntity.class).get()
                .map(SearchHit::getContent)
                .toList();
    }

    @Override
    public SearchHits<StudyRecordEntity> findAmounts(StudyRecordAmountSearchParameters searchParams) {

        BoolQuery.Builder query = new BoolQuery.Builder();

        if(StringUtils.hasText(searchParams.getSendingOrganisation())) {
            query.must(q -> new Query.Builder().term(tq -> tq
                .field("sendingOrganisation")
                .value(searchParams.getSendingOrganisation())));
        }

        if(StringUtils.hasText(searchParams.getReceivingOrganisation())) {
            query.must(q -> new Query.Builder().term(tq -> tq
                .field("receivingOrganisation")
                .value(searchParams.getReceivingOrganisation())));
        }

        if(searchParams.getCompletionDateStart() != null) {
            query.must(q -> new Query.Builder().nested(nq -> nq
                .path("completedCredits")
                .query(q2 -> new Query.Builder().range(rq -> rq
                    .field("completedCredits.completionDate")
                    .from(DateUtils.getFormatter().format(searchParams.getCompletionDateStart()))))
                .scoreMode(ChildScoreMode.None)));
        }

        if(searchParams.getCompletionDateEnd() != null) {
            query.must(q -> new Query.Builder().nested(nq -> nq
                .path("completedCredits")
                .query(q2 -> new Query.Builder().range(rq -> rq
                    .field("completedCredits.completionDate")
                    .to(DateUtils.getFormatter().format(searchParams.getCompletionDateEnd()))))
                .scoreMode(ChildScoreMode.None)));
        }

        NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder()
            .withMaxResults(0)
            .withQuery(query.build()._toQuery());

        Aggregation.Builder.ContainerBuilder groupByAggregation = null;

        if(searchParams.getGroupBy() == StudyRecordGrouping.SENDING_ORGANISATION) {
            groupByAggregation = new Aggregation.Builder().terms(ta -> ta
                    .field("sendingOrganisation"));
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.RECEIVING_ORGANISATION) {
            groupByAggregation = new Aggregation.Builder().terms(ta -> ta
                    .field("receivingOrganisation"));
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.DATES) {
            String datePattern = "uuuu-MM-dd";

            DateRangeAggregation.Builder dateRangeAggBuilder = new DateRangeAggregation.Builder()
                .field("completedCredits.completionDate")
                .keyed(true)
                .format(datePattern);

            for(StudyRecordGroupingDates dates : searchParams.getGroupByDates()) {
                String formattedStart = DateUtils.getFormatted(datePattern, dates.getStart());
                String formattedEnd = DateUtils.getFormatted(datePattern, dates.getEnd());
                String rangeKey = String.format("%s:%s", formattedStart, formattedEnd);
                dateRangeAggBuilder.ranges(DateRangeExpression.of(dre -> dre
                    .key(rangeKey)
                    .from(from -> from.expr(formattedStart))
                    .to(to -> to.expr(formattedEnd))));
            }

            nativeQueryBuilder.withAggregation("dates", new Aggregation.Builder().nested(NestedAggregation.of(na -> na
                            .path("completedCredits")))
                    .aggregations("dateRanges", dateRangeAggBuilder.build()._toAggregation())
                    .build());
        } else if(searchParams.getGroupBy() == StudyRecordGrouping.STUDYELEMENT_IDENTIFIER_CODE) {
            nativeQueryBuilder.withAggregation("completedCredits",
                    new Aggregation.Builder().nested(NestedAggregation.of(na -> na
                                    .path("completedCredits")))
                            .aggregations("studyElementIdentifierCodes", TermsAggregation.of(ta -> ta
                                    .field("completedCredits.completedCreditTarget.completedCreditTargetIdentifierCode"))._toAggregation())
                            .build());
        }

        if(groupByAggregation != null) {
            if (searchParams.getDivideBy() == StudyRecordDividing.GRADING) {
                Aggregation approvedAggregation = new Aggregation.Builder().nested(na -> na
                        .path("completedCredits"))
                    .aggregations("approvedFilter", AggregationBuilders.filter(fa -> fa
                            .bool(bq1 -> bq1
                                .must(q -> q
                                    .exists(eq -> eq
                                        .field("completedCredits.assessment.grade.code")))
                                .must(q -> q
                                    .bool(bq -> bq.mustNot(QueryBuilders.term(tq -> tq
                                        .field("completedCredits.assessment.grade.code")
                                        .value("HYL"))))))))
                    .build();

                Aggregation rejectedAggregation = new Aggregation.Builder().nested(na -> na
                        .path("completedCredits"))
                    .aggregations("rejectedFilter", AggregationBuilders.filter(fa -> fa
                        .bool(bq1 -> bq1
                            .must(q -> q
                                .exists(eq -> eq
                                    .field("completedCredits.assessment.grade.code")))
                            .must(q -> q
                                .term(tq -> tq
                                    .field("completedCredits.assessment.grade.code")
                                    .value("HYL"))))))
                    .build();

                Aggregation ungradedAggregation = new Aggregation.Builder().nested(na -> na                     
                        .path("completedCredits"))
                    .aggregations("ungradedFilter", AggregationBuilders.filter(fa -> fa
                        .bool(bq1 -> bq1
                            .mustNot(q -> q
                                .exists(eq -> eq
                                    .field("completedCredits.assessment.grade.code"))))))
                    .build();
                
                groupByAggregation.aggregations("approved", approvedAggregation);
                groupByAggregation.aggregations("rejected", rejectedAggregation);
                groupByAggregation.aggregations("ungraded", ungradedAggregation);

                nativeQueryBuilder.withAggregation("organisation", groupByAggregation.build());
            } else if (searchParams.getDivideBy() == StudyRecordDividing.MIN_EDU_GUIDANCE_AREA) {
                groupByAggregation.aggregations("minEduGuidanceArea", new Aggregation.Builder().nested(na -> na
                        .path("completedCredits"))
                    .aggregations("code", AggregationBuilders.terms(ta -> ta
                        .field("completedCredits.minEduGuidanceArea")))
                    .build());

                nativeQueryBuilder.withAggregation("organisation", groupByAggregation.build());
            } else if (searchParams.getDivideBy() == StudyRecordDividing.ORGANISATION_RESPONSIBLE_FOR_COMPLETION) {
                groupByAggregation.aggregations("organisationResponsibleForCompletion", new Aggregation.Builder().nested(na -> na
                        .path("completedCredits"))
                    .aggregations("organisationTkCode", AggregationBuilders.terms(ta -> ta
                        .field("completedCredits.organisationResponsibleForCompletion.organisationTkCode")))
                    .build());

                nativeQueryBuilder.withAggregation("organisation", groupByAggregation.build());
            }
        }

        return elasticsearchTemplate.search(nativeQueryBuilder.build(), StudyRecordEntity.class);
    }
}
