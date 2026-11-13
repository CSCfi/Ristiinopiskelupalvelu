package fi.uta.ristiinopiskelu.persistence.config;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * String deserializer that sanitizes incoming strings (all HTML tags will be removed in order to prevent potential
 * XSS attacks and to keep content clean).
 */
public class HtmlSanitizingStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();

        if(StringUtils.hasText(value)) {
            String cleaned = Jsoup.clean(value, "", new Safelist()
                    .addTags("sup", "sub", "u", "b", "i", "br", "p", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6", "a")
                    .addAttributes("a", "href", "target")
                    .addProtocols("a", "href", "http", "https", "mailto"),
                new Document.OutputSettings().prettyPrint(false));

            return Parser.unescapeEntities(cleaned, false);
        }

        return value;
    }
}
