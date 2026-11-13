package fi.uta.ristiinopiskelu.messaging.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class JsonPatcherUtilImpl implements JsonPatcherUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonPatcherUtilImpl.class);

    private JsonMapper jsonMapper;
    private String patchFilePath;

    public JsonPatcherUtilImpl(JsonMapper jsonMapper, String patchFilePath) {
        Assert.notNull(jsonMapper, "ObjectMapper cannot be null");

        this.jsonMapper = jsonMapper;
        this.patchFilePath = patchFilePath;
    }

    public JsonPatcherUtilImpl(JsonMapper jsonMapper) {
        this(jsonMapper, null);
    }

    @Override
    public <T> JsonNode patch(Object source, Class<T> targetType) throws Exception {
        JsonNode sourceJson = this.jsonMapper.valueToTree(source);
        return this.patch(sourceJson, targetType);
    }

    @Override
    public <T> JsonNode patch(JsonNode sourceJson, Class<T> targetType) throws Exception {
        throw new UnsupportedOperationException("Patching is not supported until zjsonpatch supports Jackson 3");
        /* ObjectReader reader = jsonMapper.readerForUpdating(targetType.getDeclaredConstructor().newInstance());
        T targetObject = reader.readValue(sourceJson);
        JsonNode targetJson = this.jsonMapper.valueToTree(targetObject);

        // then, apply json patch. use provided file if available, else attempt automagically
        JsonNode patch = null;

        if(StringUtils.hasText(this.patchFilePath)) {
            try {
                InputStream patchFile = this.getClass().getResourceAsStream(this.patchFilePath);

                if (patchFile != null) {
                    logger.info("Using provided JSON patch '{}'", this.patchFilePath);
                    patch = this.jsonMapper.readTree(patchFile);
                    JsonPatch.validate(patch);
                }
            } catch (Exception e) {
                throw e;
            }
        }

        // attempt automagic patch
        if(patch == null) {
            logger.info("No JSON patch provided, generating it with best effort");
            patch = JsonDiff.asJson(sourceJson, targetJson);
        }

        logger.info("Applying JSON patch:\n{}", this.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patch));

        return JsonPatch.apply(patch, sourceJson); */
    }
}
