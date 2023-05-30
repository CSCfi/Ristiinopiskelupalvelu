package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.TeachingLanguage;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.schema.converter.AbstractObjectMappingObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStudyElementRequestV8ToV9Converter<S, T> extends AbstractObjectMappingObjectConverter<S, T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractStudyElementRequestV8ToV9Converter.class);

    @Override
    public T convertObject(Object object) throws ObjectConversionException {
        JsonNode jsonNode = convertJson(getObjectMapper().valueToTree(object));
        try {
            return getObjectMapper().treeToValue(jsonNode, getTargetType());
        } catch (JsonProcessingException e) {
            throw new ObjectConversionException("Error while converting object", e);
        }
    }

    protected JsonNode convertJson(String rootElementName, JsonNode json) throws ObjectConversionException {
        if(json.hasNonNull(rootElementName)) {
            JsonNode rootElement = json.get(rootElementName);
            if(rootElement instanceof ArrayNode rootElements) {
                for(JsonNode element : rootElements) {
                    handleRootElement(element);
                }
            } else {
                handleRootElement(rootElement);
            }
        }

        return json;
    }

    private void handleRootElement(JsonNode rootElement) {
        handleInvalidTeachingLanguages("teachingLanguage", rootElement);
        handleInvalidTeachingLanguages("languagesOfCompletion", rootElement);
        handleRealisations(rootElement);
        handleCompletionOptions(rootElement);

        if(rootElement.hasNonNull("subElements")) {
            ArrayNode subElements = (ArrayNode) rootElement.get("subElements");
            for(JsonNode subElement : subElements) {
                handleInvalidTeachingLanguages("teachingLanguage", subElement);
                handleInvalidTeachingLanguages("languagesOfCompletion", subElement);
                handleRealisations(subElement);
                handleCompletionOptions(subElement);
            }
        }
    }

    private void handleRealisations(JsonNode subElement) {
        if (subElement.hasNonNull("realisations")) {
            ArrayNode realisations = (ArrayNode) subElement.get("realisations");
            for (JsonNode realisation : realisations) {
                handleInvalidTeachingLanguages("teachingLanguage", realisation);
            }
        }
    }

    private void handleCompletionOptions(JsonNode subElement) {
        if(subElement.hasNonNull("completionOptions")) {
            ArrayNode completionOptions = (ArrayNode) subElement.get("completionOptions");
            for(JsonNode completionOption : completionOptions) {
                if(completionOption.hasNonNull("assessmentItems")) {
                    ArrayNode assessmentItems = (ArrayNode) completionOption.get("assessmentItems");
                    for(JsonNode assessmentItem : assessmentItems) {
                        handleRealisations(assessmentItem);
                    }
                }
            }
        }
    }

    protected void handleInvalidTeachingLanguages(String fieldName, JsonNode jsonNode) {
        ObjectNode node = (ObjectNode) jsonNode;
        if(node.hasNonNull(fieldName)) {
            try {
                ArrayNode teachingLanguages = (ArrayNode) node.get(fieldName);
                if(!teachingLanguages.isEmpty()) {
                    node.remove(fieldName);
                    ArrayNode newTeachingLanguages = node.putArray(fieldName);
                    for(JsonNode teachingLanguage : teachingLanguages) {
                        String teachingLanguageText = teachingLanguage.asText();
                        TeachingLanguage validTeachingLanguage = TeachingLanguage.fromValue(teachingLanguageText);
                        if(validTeachingLanguage != null) {
                            newTeachingLanguages.add(validTeachingLanguage.getValue());
                        } else {
                            logger.warn("Removed invalid language '{}' when converting from {} message to {}",
                                teachingLanguageText, getSourceType().getSimpleName(), getTargetType().getSimpleName());
                        }
                    }
                }
            } catch (Exception e) {
                throw new ObjectConversionException("Error while converting object", e);
            }
        }
    }
}
