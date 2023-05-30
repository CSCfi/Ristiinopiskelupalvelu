package fi.uta.ristiinopiskelu.persistence.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * String deserializer that sanitizes incoming strings (all HTML tags will be removed in order to prevent potential
 * XSS attacks and to keep content clean).
 */
@JsonComponent
public class HtmlSanitizingStringDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String value = p.getValueAsString();

        if(StringUtils.hasText(value)) {
            return Jsoup.clean(value, Whitelist.none());
        }

        return value;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return this;
    }
}
