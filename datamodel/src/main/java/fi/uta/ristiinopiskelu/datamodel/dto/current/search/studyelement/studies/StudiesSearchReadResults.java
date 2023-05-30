package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated.SimpleAggregationDTO;

import java.util.ArrayList;
import java.util.List;

public class StudiesSearchReadResults extends ListSearchResults<AbstractStudyElementReadDTO> {

    private List<SimpleAggregationDTO> aggregations = new ArrayList<>();
    private long totalHits;

    public StudiesSearchReadResults() {
        super();
    }

    public StudiesSearchReadResults(List<AbstractStudyElementReadDTO> results, List<SimpleAggregationDTO> aggregations, long totalHits) {
        super(results);
        this.aggregations = aggregations;
        this.totalHits = totalHits;
    }

    public List<SimpleAggregationDTO> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<SimpleAggregationDTO> aggregations) {
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }
}
