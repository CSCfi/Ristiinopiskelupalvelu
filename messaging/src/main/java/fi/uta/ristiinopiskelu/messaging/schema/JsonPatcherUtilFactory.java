package fi.uta.ristiinopiskelu.messaging.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JsonPatcherUtilFactory {

    @Autowired
    private ObjectMapper objectMapper;

    public JsonPatcherUtil create(String patchFilePath) {
        return new JsonPatcherUtilImpl(objectMapper, patchFilePath);
    }

    public JsonPatcherUtil create() {
        return new JsonPatcherUtilImpl(objectMapper, null);
    }
}
