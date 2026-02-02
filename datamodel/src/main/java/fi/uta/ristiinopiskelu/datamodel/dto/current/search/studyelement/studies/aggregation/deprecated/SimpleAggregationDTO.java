package fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.deprecated;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.aggregation.AggregationType;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

// use "AggregationDTO" now
@Schema(name = "SimpleAggregationV9", title = "SimpleAggregationV9", type = "object", discriminatorProperty = "type", discriminatorMapping = {
    @DiscriminatorMapping(value = "MULTI", schema = SimpleMultiBucketAggregationDTO.class),
    @DiscriminatorMapping(value = "SINGLE", schema = SimpleSingleBucketAggregationDTO.class)
}, subTypes = { SimpleMultiBucketAggregationDTO.class, SimpleSingleBucketAggregationDTO.class })
@Deprecated(since = "9.0.0")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = SimpleMultiBucketAggregationDTO.class, name = "MULTI"),
    @JsonSubTypes.Type(value = SimpleSingleBucketAggregationDTO.class, name = "SINGLE")
})
public abstract class SimpleAggregationDTO {
    
    private AggregationType type;

    public AggregationType getType() {
        return type;
    }

    public void setType(AggregationType type) {
        this.type = type;
    }

    public abstract String getName();
}
