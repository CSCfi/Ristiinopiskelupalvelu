package fi.uta.ristiinopiskelu.messaging.message.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.exception.JsonTemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStreamReader;

public class JsonTemplateProducer {

    private static final Logger logger = LoggerFactory.getLogger(JsonTemplateProducer.class);

    @Autowired
    private ObjectMapper objectMapper;

    public String getTemplate(fi.uta.ristiinopiskelu.messaging.message.v8.MessageType messageType) {
        String json = null;

        try {
            String groovyScriptFilePath = String.format("/performancetests/groovy/%s/%sTemplate.groovy",
                    MessageType.getMessageGroup(messageType).name().toLowerCase(), messageType.getClazz().getSimpleName());

            json = this.executeGroovyScript(groovyScriptFilePath);
        } catch (JsonTemplateException e) {
            logger.error("Error while generating JSON template for message type " + messageType, e);
        }

        if(StringUtils.isEmpty(json)) {
            logger.info("Could not get JSON template from groovy script, falling back to basic serialization");
            Assert.notNull(objectMapper, "ObjectMapper cannot be null");

            try {
                json = objectMapper.writeValueAsString(messageType.getClazz().getConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Error while serializing message type " + messageType + " class", e);
            }
        }

        return json;
    }

    private String executeGroovyScript(String groovyScriptFilePath) throws JsonTemplateException {
        Assert.hasText(groovyScriptFilePath, "Groovy script file path cannot be empty");
        
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("groovy");
        String json;

        try {
            json = (String) engine.eval(new InputStreamReader(getClass().getResourceAsStream(groovyScriptFilePath)));
        } catch (Exception e) {
            throw new JsonTemplateException("Cannot evaluate groovy script file: " + groovyScriptFilePath, e);
        }

        return json;
    }
}
