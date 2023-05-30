package fi.uta.ristiinopiskelu.messaging.schema.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import org.modelmapper.ModelMapper;
import org.springframework.util.Assert;

public class DefaultObjectConverter implements ObjectConverter {

    private ModelMapper modelMapper;
    private ObjectMapper objectMapper;
    private Class<?> sourceType;
    private Class<?> targetType;

    @Override
    public Class<?> getSourceType() {
        return this.sourceType;
    }

    @Override
    public Class<?> getTargetType() {
        return this.targetType;
    }

    public DefaultObjectConverter(ModelMapper modelMapper, ObjectMapper objectMapper, Class<?> sourceType, Class<?> targetType) {
        Assert.notNull(modelMapper, "modelMapper cannot be null");
        Assert.notNull(objectMapper, "objectMapper cannot be null");
        Assert.notNull(sourceType, "sourceType cannot be null");
        Assert.notNull(targetType, "targetType cannot be null");

        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public Object convertObject(Object object) throws ObjectConversionException {
        return this.modelMapper.map(object, getTargetType());
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return json;
   }
}
