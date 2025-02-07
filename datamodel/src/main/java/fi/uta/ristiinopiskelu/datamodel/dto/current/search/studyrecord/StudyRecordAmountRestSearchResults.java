package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "StudyRecordAmountSearchResults")
public class StudyRecordAmountRestSearchResults {

    private long totalHits;
    private List<AggregationDTO> aggregations;

    public StudyRecordAmountRestSearchResults() {
    }

    public StudyRecordAmountRestSearchResults(long totalHits, List<AggregationDTO> aggregations) {
        this.totalHits = totalHits;
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public List<AggregationDTO> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationDTO> aggregations) {
        this.aggregations = aggregations;
    }
}
