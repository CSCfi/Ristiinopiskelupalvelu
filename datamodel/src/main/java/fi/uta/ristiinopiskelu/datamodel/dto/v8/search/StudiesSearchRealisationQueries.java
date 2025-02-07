package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

public class StudiesSearchRealisationQueries {

     private BoolQuery.Builder finalQuery;
     private BoolQuery.Builder mainRealisationQuery;
     private BoolQuery.Builder mainAssessmentItemRealisationQuery;

    public StudiesSearchRealisationQueries(BoolQuery.Builder finalQuery, BoolQuery.Builder mainRealisationQuery,
                                           BoolQuery.Builder mainAssessmentItemRealisationQuery) {
        this.finalQuery = finalQuery;
        this.mainRealisationQuery = mainRealisationQuery;
        this.mainAssessmentItemRealisationQuery = mainAssessmentItemRealisationQuery;
    }

    public BoolQuery.Builder getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(BoolQuery.Builder finalQuery) {
        this.finalQuery = finalQuery;
    }

    public BoolQuery.Builder getMainRealisationQuery() {
        return mainRealisationQuery;
    }

    public void setMainRealisationQuery(BoolQuery.Builder mainRealisationQuery) {
        this.mainRealisationQuery = mainRealisationQuery;
    }

    public BoolQuery.Builder getMainAssessmentItemRealisationQuery() {
        return mainAssessmentItemRealisationQuery;
    }

    public void setMainAssessmentItemRealisationQuery(BoolQuery.Builder mainAssessmentItemRealisationQuery) {
        this.mainAssessmentItemRealisationQuery = mainAssessmentItemRealisationQuery;
    }
}
