package fi.csc.ristiinopiskelu.admin.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class TrimmingStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        String value = jsonParser.getValueAsString();
        return value == null ? null : value.trim();
    }
}
