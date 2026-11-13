package fi.uta.ristiinopiskelu.datamodel.dto.v8.code;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.Language;
import fi.uta.ristiinopiskelu.datamodel.entity.common.code.CodeValueEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Schema(name = "CodeValueV8", title = "CodeValueV8")
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

    public static List<CodeValue> fromV8(List<fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeValue> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .filter(Objects::nonNull)
            .map(value -> {
                fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeValue target = new fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeValue();
                target.setLanguage(Language.fromValue(value.getLanguage().getValue()));
                target.setValue(value.getValue());
                target.setAbbreviation(value.getAbbreviation());
                target.setDescription(value.getDescription());
                return target;
            }).collect(Collectors.toList());
    }

    public static List<CodeValue> from(List<CodeValueEntity> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .filter(Objects::nonNull)
            .map(value -> {
                fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeValue target = new fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeValue();
                target.setLanguage(value.getLanguage() == null ? null : fi.uta.ristiinopiskelu.datamodel.dto.v8.Language.valueOf(value.getLanguage().name()));
                target.setValue(value.getValue());
                target.setAbbreviation(value.getAbbreviation());
                target.setDescription(value.getDescription());
                return target;
            }).collect(Collectors.toList());
    }
}
