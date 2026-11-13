package fi.uta.ristiinopiskelu.messaging.schema;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JsonPatcherUtilFactory {

    @Autowired
    private JsonMapper jsonMapper;

    public JsonPatcherUtil create(String patchFilePath) {
        return new JsonPatcherUtilImpl(jsonMapper, patchFilePath);
    }

    public JsonPatcherUtil create() {
        return new JsonPatcherUtilImpl(jsonMapper, null);
    }
}
