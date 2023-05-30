package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.search.AggregationDTO;

import java.util.ArrayList;
import java.util.List;

public class StudiesRestSearchResults extends ListSearchResults<StudyElementRestDTO> {

    private List<AggregationDTO> aggregations = new ArrayList<>();
    private long totalHits;

    public StudiesRestSearchResults() {
        super();
    }

    public StudiesRestSearchResults(List<StudyElementRestDTO> results, List<AggregationDTO> aggregations, long totalHits) {
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
