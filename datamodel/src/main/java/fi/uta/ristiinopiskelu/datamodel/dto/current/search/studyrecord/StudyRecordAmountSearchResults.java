package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;

@Schema(name = "StudyRecordAmountSearchResultsV9", title = "StudyRecordAmountSearchResultsV9")
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
