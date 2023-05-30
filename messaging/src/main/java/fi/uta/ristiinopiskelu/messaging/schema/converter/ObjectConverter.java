package fi.uta.ristiinopiskelu.messaging.schema.converter;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;

public interface ObjectConverter<S, T> {

    Class<S> getSourceType();

    Class<T> getTargetType();

    T convertObject(Object object) throws ObjectConversionException;

    JsonNode convertJson(JsonNode json) throws ObjectConversionException;
}
