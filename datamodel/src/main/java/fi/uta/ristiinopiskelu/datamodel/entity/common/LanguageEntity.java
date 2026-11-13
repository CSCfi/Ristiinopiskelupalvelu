package fi.uta.ristiinopiskelu.datamodel.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LanguageEntity {

    FI("fi"),
    SV("sv"),
    EN("en");

    private final String value;

    LanguageEntity(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LanguageEntity fromValue(String text) {
        for (LanguageEntity b : LanguageEntity.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    // Show values correctly in Swagger
    @Override
    public String toString() {
        return this.value;
    }
}
