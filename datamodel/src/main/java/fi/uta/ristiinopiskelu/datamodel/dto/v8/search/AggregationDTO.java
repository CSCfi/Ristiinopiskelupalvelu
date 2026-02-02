package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AggregationV8", title = "AggregationV8", type = "object", discriminatorProperty = "type", discriminatorMapping = {
    @DiscriminatorMapping(value = "MULTI", schema = MultiBucketAggregationDTO.class),
    @DiscriminatorMapping(value = "SINGLE", schema = SingleBucketAggregationDTO.class)
}, subTypes = { MultiBucketAggregationDTO.class, SingleBucketAggregationDTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = MultiBucketAggregationDTO.class, name = "MULTI"),
    @JsonSubTypes.Type(value = SingleBucketAggregationDTO.class, name = "SINGLE")
})
public abstract class AggregationDTO {
    
    private AggregationType type;

    public AggregationType getType() {
        return type;
    }

    public void setType(AggregationType type) {
        this.type = type;
    }

    public abstract String getName();
}
