package fi.uta.ristiinopiskelu.datamodel.dto.v8.search;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
