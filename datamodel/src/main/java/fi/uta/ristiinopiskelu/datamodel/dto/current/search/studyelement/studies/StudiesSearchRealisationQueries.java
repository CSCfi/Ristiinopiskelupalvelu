package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies;

import org.elasticsearch.index.query.BoolQueryBuilder;

public class StudiesSearchRealisationQueries {

     private BoolQueryBuilder finalQuery;
     private BoolQueryBuilder aggregationRealisationQuery;
     private BoolQueryBuilder aggregationAssessmentItemRealisationQuery;

    public StudiesSearchRealisationQueries(BoolQueryBuilder finalQuery, BoolQueryBuilder aggregationRealisationQuery,
                                           BoolQueryBuilder aggregationAssessmentItemRealisationQuery) {
        this.finalQuery = finalQuery;
        this.aggregationRealisationQuery = aggregationRealisationQuery;
        this.aggregationAssessmentItemRealisationQuery = aggregationAssessmentItemRealisationQuery;
    }

    public BoolQueryBuilder getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(BoolQueryBuilder finalQuery) {
        this.finalQuery = finalQuery;
    }

    public BoolQueryBuilder getAggregationRealisationQuery() {
        return aggregationRealisationQuery;
    }

    public void setAggregationRealisationQuery(BoolQueryBuilder aggregationRealisationQuery) {
        this.aggregationRealisationQuery = aggregationRealisationQuery;
    }

    public BoolQueryBuilder getAggregationAssessmentItemRealisationQuery() {
        return aggregationAssessmentItemRealisationQuery;
    }

    public void setAggregationAssessmentItemRealisationQuery(BoolQueryBuilder aggregationAssessmentItemRealisationQuery) {
        this.aggregationAssessmentItemRealisationQuery = aggregationAssessmentItemRealisationQuery;
    }
}
