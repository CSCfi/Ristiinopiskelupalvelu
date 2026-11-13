package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.KeywordEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "KeywordV9", title = "KeywordV9")
public class Keyword {

    private String key;
    private LocalisedString value;
    private String keySet;
    private LocalisedString keySetValue;

    /**
     * M2 2.11.1 esim: iscedCode, erasmus subjectArea
     *
     * @return key
     **/
    @Schema(description = "M2 2.11.1 esim: iscedCode, erasmus subjectArea")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * M2 2.11.2
     *
     * @return keyword
     **/
    @Schema(required = true, description = "M2 2.11.2")
    public LocalisedString getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(LocalisedString value) {
        this.value = value;
    }

    /**
     * @return String return the keySet
     */
    public String getKeySet() {
        return keySet;
    }

    /**
     * @param keySet the keySet to set
     */
    public void setKeySet(String keySet) {
        this.keySet = keySet;
    }

    public LocalisedString getKeySetValue() {
        return keySetValue;
    }

    public void setKeySetValue(LocalisedString keySetValue) {
        this.keySetValue = keySetValue;
    }

    public static Keyword from(KeywordEntity source) {
        if (source == null) {
            return null;
        }
        Keyword target = new Keyword();
        target.setKey(source.getKey());
        target.setValue(LocalisedString.from(source.getValue()));
        target.setKeySet(source.getKeySet());
        target.setKeySetValue(LocalisedString.from(source.getKeySetValue()));
        return target;
    }

    public static List<Keyword> from(List<KeywordEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(Keyword::from).toList();
    }
}
