package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;

public class CodeValue {

    private Language language;
    private String value;
    private String abbreviation;
    private String description;

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDescription() {
        return description;
    }

    public Language getLanguage() {
        return language;
    }

    public String getValue() {
        return value;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
