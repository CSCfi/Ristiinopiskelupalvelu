package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

public class StudiesSearchRealisationQueries {

     private Query finalQuery;
     private BoolQuery.Builder aggregationRealisationQuery;
     private BoolQuery.Builder aggregationAssessmentItemRealisationQuery;

    public StudiesSearchRealisationQueries(Query finalQuery, BoolQuery.Builder aggregationRealisationQuery,
                                           BoolQuery.Builder aggregationAssessmentItemRealisationQuery) {
        this.finalQuery = finalQuery;
        this.aggregationRealisationQuery = aggregationRealisationQuery;
        this.aggregationAssessmentItemRealisationQuery = aggregationAssessmentItemRealisationQuery;
    }

    public Query getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(Query finalQuery) {
        this.finalQuery = finalQuery;
    }

    public BoolQuery.Builder getAggregationRealisationQuery() {
        return aggregationRealisationQuery;
    }

    public void setAggregationRealisationQuery(BoolQuery.Builder aggregationRealisationQuery) {
        this.aggregationRealisationQuery = aggregationRealisationQuery;
    }

    public BoolQuery.Builder getAggregationAssessmentItemRealisationQuery() {
        return aggregationAssessmentItemRealisationQuery;
    }

    public void setAggregationAssessmentItemRealisationQuery(BoolQuery.Builder aggregationAssessmentItemRealisationQuery) {
        this.aggregationAssessmentItemRealisationQuery = aggregationAssessmentItemRealisationQuery;
    }
}
