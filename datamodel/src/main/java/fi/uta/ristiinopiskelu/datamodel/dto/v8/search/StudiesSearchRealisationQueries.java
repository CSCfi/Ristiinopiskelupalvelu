package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import org.elasticsearch.index.query.BoolQueryBuilder;

public class StudiesSearchRealisationQueries {

     private BoolQueryBuilder finalQuery;
     private BoolQueryBuilder mainRealisationQuery;
     private BoolQueryBuilder mainAssessmentItemRealisationQuery;

    public StudiesSearchRealisationQueries(BoolQueryBuilder finalQuery, BoolQueryBuilder mainRealisationQuery,
                                           BoolQueryBuilder mainAssessmentItemRealisationQuery) {
        this.finalQuery = finalQuery;
        this.mainRealisationQuery = mainRealisationQuery;
        this.mainAssessmentItemRealisationQuery = mainAssessmentItemRealisationQuery;
    }

    public BoolQueryBuilder getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(BoolQueryBuilder finalQuery) {
        this.finalQuery = finalQuery;
    }

    public BoolQueryBuilder getMainRealisationQuery() {
        return mainRealisationQuery;
    }

    public void setMainRealisationQuery(BoolQueryBuilder mainRealisationQuery) {
        this.mainRealisationQuery = mainRealisationQuery;
    }

    public BoolQueryBuilder getMainAssessmentItemRealisationQuery() {
        return mainAssessmentItemRealisationQuery;
    }

    public void setMainAssessmentItemRealisationQuery(BoolQueryBuilder mainAssessmentItemRealisationQuery) {
        this.mainAssessmentItemRealisationQuery = mainAssessmentItemRealisationQuery;
    }
}
