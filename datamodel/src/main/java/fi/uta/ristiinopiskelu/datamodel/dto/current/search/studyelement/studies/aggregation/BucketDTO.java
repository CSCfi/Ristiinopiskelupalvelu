package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "Bucket")
public class BucketDTO {
    private String key;
    private long count;
    private List<AggregationDTO> aggregations = new ArrayList<>();

    public BucketDTO() {
        
    }

    public BucketDTO(String key, long count) {
        this.key = key;
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public String getKey() {
        return key;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<AggregationDTO> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationDTO> aggregations) {
        this.aggregations = aggregations;
    }
}
