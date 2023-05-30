package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated;

import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

// use "MultiBucketAggregationDTO" now
@Schema(name = "SimpleMultiBucketAggregation")
@Deprecated(since = "9.0.0")
public class SimpleMultiBucketAggregationDTO extends SimpleAggregationDTO {

    private String name;
    private List<SimpleBucketDTO> buckets = new ArrayList<>();

    public SimpleMultiBucketAggregationDTO() {
        setType(AggregationType.MULTI);
    }

    public SimpleMultiBucketAggregationDTO(String name, List<SimpleBucketDTO> buckets) {
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

    public void setBuckets(List<SimpleBucketDTO> buckets) {
        this.buckets = buckets;
    }

    public List<SimpleBucketDTO> getBuckets() {
        return buckets;
    }
}
