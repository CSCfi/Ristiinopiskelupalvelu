package fi.uta.ristiinopiskelu.datamodel.dto.current.common.code;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeValueEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

@Schema(name = "CodeValueV9", title = "CodeValueV9")
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

    public static List<CodeValue> from(List<CodeValueEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(value -> {
            if (value == null) {
                return null;
            }
            fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeValue target =
                new fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeValue();
            target.setLanguage(value.getLanguage() == null ? null : Language.valueOf(value.getLanguage().name()));
            target.setValue(value.getValue());
            target.setAbbreviation(value.getAbbreviation());
            target.setDescription(value.getDescription());
            return target;
        }).collect(Collectors.toList());
    }
}
