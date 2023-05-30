package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import org.elasticsearch.search.aggregations.Aggregations;

public class StudyRecordAmountSearchResults {

    private long totalHits;
    private Aggregations aggregations;

    public StudyRecordAmountSearchResults(long totalHits, Aggregations aggregations) {
        this.totalHits = totalHits;
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public Aggregations getAggregations() {
        return aggregations;
    }

    public void setAggregations(Aggregations aggregations) {
        this.aggregations = aggregations;
    }
}
