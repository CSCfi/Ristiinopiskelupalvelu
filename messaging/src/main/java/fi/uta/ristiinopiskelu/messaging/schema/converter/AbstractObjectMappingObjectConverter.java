package fi.uta.ristiinopiskelu.messaging.schema.converter;

import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public abstract class AbstractObjectMappingObjectConverter<S, T> implements ObjectConverter {

    @Autowired
    private JsonMapper jsonMapper;

    @Override
    public abstract Class<S> getSourceType();

    @Override
    public abstract Class<T> getTargetType();

    protected JsonMapper getJsonMapper() {
        return jsonMapper;
    }
    
    @Override
    public T convertObject(Object object) throws ObjectConversionException {
        try {
            return getJsonMapper().convertValue(object, getTargetType());
        } catch (IllegalArgumentException e) {
            throw new ObjectConversionException("Error while converting object", e);
        }
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return json;
    }
}
