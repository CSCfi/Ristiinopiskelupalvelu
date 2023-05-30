package fi.uta.ristiinopiskelu.datamodel.dto.v8;

public class Description {

    private String key;
    private LocalisedString name = null;
    private LocalisedString value = null;

    /**
     * @return String return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return LocalisedString return the name
     */
    public LocalisedString getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(LocalisedString name) {
        this.name = name;
    }

    /**
     * @return LocalisedString return the value
     */
    public LocalisedString getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(LocalisedString value) {
        this.value = value;
    }

}
