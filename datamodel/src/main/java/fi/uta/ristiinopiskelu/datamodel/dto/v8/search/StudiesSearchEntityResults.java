package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.ListSearchResults;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import org.elasticsearch.search.aggregations.Aggregations;

import java.util.List;

public class StudiesSearchEntityResults extends ListSearchResults<StudyElementEntity> {

    private Aggregations aggregations;
    private long totalHits;

    public StudiesSearchEntityResults() {
        super();
    }

    public StudiesSearchEntityResults(List<StudyElementEntity> results, Aggregations aggregations, long totalHits) {
        super(results);
        this.aggregations = aggregations;
        this.totalHits = totalHits;
    }

    public Aggregations getAggregations() {
        return aggregations;
    }

    public void setAggregations(Aggregations aggregations) {
        this.aggregations = aggregations;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }
}
