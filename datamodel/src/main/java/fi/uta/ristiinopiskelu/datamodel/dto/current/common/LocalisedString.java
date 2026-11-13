package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
@Schema(name = "LocalisedStringV9", title = "LocalisedStringV9")
public class LocalisedString {
    
    /**
     * Map containing localized strings, fi,sv,en... ISO 639.
     **/
    private HashMap<Language, String> values;

    public LocalisedString() {
        values = new HashMap<>();
    }

    public LocalisedString(String fi) {
        this();
        if (fi != null) {
            values.put(Language.FI, fi);
        }
    }

    public LocalisedString(String fi, String en, String sv) {
        this();
        if (fi != null) {
            values.put(Language.FI, fi);
        }
        if (en != null) {
            values.put(Language.EN, en);
        }
        if (sv != null) {
            values.put(Language.SV, sv);
        }
    }

    @Deprecated
    public String getValue(String locale) {
        return getValue(Language.fromValue(locale));
    }

    public String getValue(Language language) {
        String value = values.get(language);
        return value == null ? "" : value;
    }

    @Deprecated
    public void addValue(String locale, String value) {
        values.put(Language.fromValue(locale), value);
    }

    public void addValue(Language language, String value) {
        values.put(language, value);
    }

    public HashMap<Language, String> getValues() {
        return values;
    }

    public void setValues(HashMap<Language, String> values) {
        this.values = values;
    }

    public static String getPreferredValue(LocalisedString localisedString, List<Language> preferredLanguageOrder) {
        Assert.notNull(localisedString, "localisedString cannot be null");
        Assert.notEmpty(preferredLanguageOrder, "preferredLanguageOrder cannot be empty");

        for(Language language : preferredLanguageOrder) {
            String value = localisedString.getValue(language);

            if(StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "LocalisedString[fi='" + values.get(Language.FI) + "', en='" + values.get(Language.EN) + "', sv='" + values.get(Language.SV) + "']";
    }

    public static LocalisedString from(fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString source) {

        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString target =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString();
        if (source.getValues() == null) {
            return target;
        }

        source.getValues().forEach((language, value) ->
            target.addValue(fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language.valueOf(language.name()), value));
        return target;
    }

    public static LocalisedString from(LocalisedStringEntity source) {
        if (source == null) {
            return null;
        }

        fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString target =
            new fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString();
        if (source.getValues() == null) {
            return target;
        }

        source.getValues().forEach((language, value) ->
            target.addValue(fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language.valueOf(language.name()), value));
        return target;
    }
}
