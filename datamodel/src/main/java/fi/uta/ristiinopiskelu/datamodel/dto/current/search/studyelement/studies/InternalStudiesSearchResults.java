package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationDTO;

import java.util.ArrayList;
import java.util.List;

public class InternalStudiesSearchResults extends ListSearchResults<AbstractStudyElementReadDTO> {

    private List<AggregationDTO> aggregations = new ArrayList<>();
    private long totalHits;

    public InternalStudiesSearchResults() {
        super();
    }

    public InternalStudiesSearchResults(List<AbstractStudyElementReadDTO> results, List<AggregationDTO> aggregations, long totalHits) {
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
