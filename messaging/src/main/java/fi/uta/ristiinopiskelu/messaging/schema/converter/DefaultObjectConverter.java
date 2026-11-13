package fi.uta.ristiinopiskelu.messaging.schema.converter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import org.springframework.util.Assert;

public class DefaultObjectConverter implements ObjectConverter {

    private JsonMapper jsonMapper;
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

    public DefaultObjectConverter(JsonMapper jsonMapper, Class<?> sourceType, Class<?> targetType) {
        Assert.notNull(jsonMapper, "jsonMapper cannot be null");
        Assert.notNull(sourceType, "sourceType cannot be null");
        Assert.notNull(targetType, "targetType cannot be null");

        this.jsonMapper = jsonMapper;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public Object convertObject(Object object) throws ObjectConversionException {
        try {
            return this.jsonMapper.convertValue(object, getTargetType());
        } catch (IllegalArgumentException e) {
            throw new ObjectConversionException("Error while converting object", e);
        }
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return json;
   }
}
