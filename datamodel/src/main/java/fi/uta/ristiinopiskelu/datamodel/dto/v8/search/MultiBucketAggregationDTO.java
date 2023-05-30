package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import java.util.ArrayList;
import java.util.List;

public class MultiBucketAggregationDTO extends AggregationDTO {

    private String name;
    private List<BucketDTO> buckets = new ArrayList<>();

    public MultiBucketAggregationDTO() {
        setType(AggregationType.MULTI);
    }

    public MultiBucketAggregationDTO(String name, List<BucketDTO> buckets) {
        this();
        this.name = name;
        this.buckets = buckets;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setBuckets(List<BucketDTO> buckets) {
        this.buckets = buckets;
    }

    public List<BucketDTO> getBuckets() {
        return buckets;
    }
}
