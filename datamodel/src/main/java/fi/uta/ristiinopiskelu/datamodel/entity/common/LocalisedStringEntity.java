package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;

public class LocalisedStringEntity {
    
    /**
     * Map containing localized strings, fi,sv,en... ISO 639.
     **/
    private HashMap<LanguageEntity, String> values;

    public LocalisedStringEntity() {
        values = new HashMap<>();
    }

    public LocalisedStringEntity(String fi) {
        this();
        if (fi != null) {
            values.put(LanguageEntity.FI, fi);
        }
    }

    public LocalisedStringEntity(String fi, String en, String sv) {
        this();
        if (fi != null) {
            values.put(LanguageEntity.FI, fi);
        }
        if (en != null) {
            values.put(LanguageEntity.EN, en);
        }
        if (sv != null) {
            values.put(LanguageEntity.SV, sv);
        }
    }

    @Deprecated
    public String getValue(String locale) {
        return getValue(LanguageEntity.fromValue(locale));
    }

    public String getValue(LanguageEntity language) {
        String value = values.get(language);
        return value == null ? "" : value;
    }

    public String getValue(fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language language) {
        if (language == null) {
            return "";
        }
        return getValue(LanguageEntity.valueOf(language.name()));
    }

    @Deprecated
    public void addValue(String locale, String value) {
        values.put(LanguageEntity.fromValue(locale), value);
    }

    public void addValue(LanguageEntity language, String value) {
        values.put(language, value);
    }

    public HashMap<LanguageEntity, String> getValues() {
        return values;
    }

    public void setValues(HashMap<LanguageEntity, String> values) {
        this.values = values;
    }

    public static String getPreferredValue(LocalisedStringEntity localisedString, List<LanguageEntity> preferredLanguageOrder) {
        Assert.notNull(localisedString, "localisedString cannot be null");
        Assert.notEmpty(preferredLanguageOrder, "preferredLanguageOrder cannot be empty");

        for(LanguageEntity language : preferredLanguageOrder) {
            String value = localisedString.getValue(language);

            if(StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "LocalisedStringEntity[fi='" + values.get(LanguageEntity.FI) + "', en='" + values.get(LanguageEntity.EN) + "', sv='" + values.get(LanguageEntity.SV) + "']";
    }

    public static LocalisedStringEntity from(LocalisedString source) {
        if (source == null) {
            return null;
        }

        LocalisedStringEntity target = new LocalisedStringEntity();
        HashMap<LanguageEntity, String> values = new HashMap<>();
        if (source.getValues() != null) {
            source.getValues().forEach((k, v) -> values.put(LanguageEntity.valueOf(k.name()), v));
        }
        target.setValues(values);
        return target;
    }

    public static LocalisedStringEntity from(fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString source) {
        if (source == null) {
            return null;
        }

        LocalisedStringEntity target = new LocalisedStringEntity();
        HashMap<LanguageEntity, String> values = new HashMap<>();

        if (source.getValues() != null) {
            source.getValues().forEach((k, v) ->
                values.put(LanguageEntity.valueOf(k.name()), v));
        }

        target.setValues(values);
        return target;
    }
}
