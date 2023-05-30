package fi.uta.ristiinopiskelu.messaging.schema.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.JsonPatcherUtilFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractJsonPatchObjectConverter<S, T> implements ObjectConverter {

    @Autowired
    private JsonPatcherUtilFactory jsonPatcherUtilFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public abstract Class<S> getSourceType();

    @Override
    public abstract Class<T> getTargetType();

    @Override
    public T convertObject(Object object) throws ObjectConversionException {
        Assert.notNull(object, "Object to be converted cannot be null");

        try {
            JsonNode jsonNode = objectMapper.valueToTree(object);
            JsonNode converted = this.convertJson(jsonNode);
            return objectMapper.reader().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).treeToValue(converted, getTargetType());
        } catch (Exception e) {
            throw new ObjectConversionException("Unable to convert object", e);
        }
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        try {
            return this.jsonPatcherUtilFactory.create(this.getPatchFilePath()).patch(json, this.getTargetType());
        } catch (Exception e) {
            throw new ObjectConversionException("Unable to convert json", e);
        }

    }

    public abstract String getPatchFilePath();
}
