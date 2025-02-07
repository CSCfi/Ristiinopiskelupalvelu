package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;

public class StudyRecordAmountSearchResults {

    private long totalHits;
    private ElasticsearchAggregations aggregations;

    public StudyRecordAmountSearchResults(long totalHits, ElasticsearchAggregations aggregations) {
        this.totalHits = totalHits;
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public ElasticsearchAggregations getAggregations() {
        return aggregations;
    }

    public void setAggregations(ElasticsearchAggregations aggregations) {
        this.aggregations = aggregations;
    }
}
