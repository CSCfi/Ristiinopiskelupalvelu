package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;

import java.util.ArrayList;
import java.util.List;

public class StudiesSearchResults extends ListSearchResults<StudyElementEntity> {

    private List<AggregationDTO> aggregations = new ArrayList<>();
    private long totalHits;

    public StudiesSearchResults() {
        super();
    }

    public StudiesSearchResults(List<StudyElementEntity> results, List<AggregationDTO> aggregations, long totalHits) {
        super(results);
        this.aggregations = aggregations;
        this.totalHits = totalHits;
    }

    public List<AggregationDTO> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationDTO> aggregations) {
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }
}
