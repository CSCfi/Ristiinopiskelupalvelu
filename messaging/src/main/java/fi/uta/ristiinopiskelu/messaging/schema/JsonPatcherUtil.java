package fi.uta.ristiinopiskelu.messaging.schema;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonPatcherUtil {

    <T> JsonNode patch(Object source, Class<T> targetType) throws Exception;

    <T> JsonNode patch(JsonNode sourceJson, Class<T> targetType) throws Exception;
}
