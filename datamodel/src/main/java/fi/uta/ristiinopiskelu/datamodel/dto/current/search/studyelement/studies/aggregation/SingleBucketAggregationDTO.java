package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SingleBucketAggregation")
public class SingleBucketAggregationDTO extends AggregationDTO {

    private String name;
    private long count;
    private List<AggregationDTO> aggregations;

    public SingleBucketAggregationDTO(String name, long count) {
        this();
        this.name = name;
        this.count = count;
    }

    public SingleBucketAggregationDTO() {
        setType(AggregationType.SINGLE);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<AggregationDTO> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationDTO> aggregations) {
        this.aggregations = aggregations;
    }
}
