package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import io.swagger.v3.oas.annotations.media.Schema;

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
}
