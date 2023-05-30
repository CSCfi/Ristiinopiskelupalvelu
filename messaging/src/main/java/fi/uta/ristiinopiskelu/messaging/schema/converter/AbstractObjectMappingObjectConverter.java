package fi.uta.ristiinopiskelu.messaging.schema.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractObjectMappingObjectConverter<S, T> implements ObjectConverter {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public abstract Class<S> getSourceType();

    @Override
    public abstract Class<T> getTargetType();

    protected ModelMapper getModelMapper() {
        return this.modelMapper;
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    @Override
    public T convertObject(Object object) throws ObjectConversionException {
        return this.modelMapper.map(object, getTargetType());
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return json;
    }
}
