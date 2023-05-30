package fi.uta.ristiinopiskelu.messaging.schema;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URL;

public class JsonPatcherUtilImpl implements JsonPatcherUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonPatcherUtilImpl.class);

    private ObjectMapper objectMapper;
    private String patchFilePath;

    public JsonPatcherUtilImpl(ObjectMapper objectMapper, String patchFilePath) {
        Assert.notNull(objectMapper, "ObjectMapper cannot be null");

        this.objectMapper = objectMapper;
        this.patchFilePath = patchFilePath;
    }

    public JsonPatcherUtilImpl(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Override
    public <T> JsonNode patch(Object source, Class<T> targetType) throws Exception {
        JsonNode sourceJson = this.objectMapper.valueToTree(source);
        return this.patch(sourceJson, targetType);
    }

    @Override
    public <T> JsonNode patch(JsonNode sourceJson, Class<T> targetType) throws Exception {
        ObjectReader reader = objectMapper.readerForUpdating(targetType.getDeclaredConstructor().newInstance()).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        T targetObject = reader.readValue(sourceJson);
        JsonNode targetJson = this.objectMapper.valueToTree(targetObject);

        // then, apply json patch. use provided file if available, else attempt automagically
        JsonNode patch = null;

        if(StringUtils.hasText(this.patchFilePath)) {
            try {
                URL patchFile = this.getClass().getResource(this.patchFilePath);

                if (patchFile != null) {
                    logger.info("Using provided JSON patch '{}'", this.patchFilePath);
                    patch = this.objectMapper.readTree(patchFile);
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

        logger.info("Applying JSON patch:\n{}", this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patch));

        return JsonPatch.apply(patch, sourceJson);
    }
}
