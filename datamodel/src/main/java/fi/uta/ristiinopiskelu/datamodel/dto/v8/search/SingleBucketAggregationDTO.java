package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SingleBucketAggregationV8", title = "SingleBucketAggregationV8", type = "object")
public class SingleBucketAggregationDTO extends AggregationDTO {

    private String name;
    private long count;

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
}
