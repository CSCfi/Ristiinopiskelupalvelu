package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationType;
import io.swagger.v3.oas.annotations.media.Schema;

// use "SingleBucketAggregationDTO" instead
@Schema(name = "SimpleSingleBucketAggregation")
@Deprecated(since = "9.0.0")
public class SimpleSingleBucketAggregationDTO extends SimpleAggregationDTO {

    private String name;
    private long count;

    public SimpleSingleBucketAggregationDTO(String name, long count) {
        this();
        this.name = name;
        this.count = count;
    }

    public SimpleSingleBucketAggregationDTO() {
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
}
