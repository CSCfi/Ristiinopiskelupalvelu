package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.DescriptionEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;

@Schema(name = "DescriptionV9", title = "DescriptionV9")
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

    public static List<Description> from(List<DescriptionEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(item -> {
            if (item == null) {
                return null;
            }
            Description target = new Description();
            target.setKey(item.getKey());
            target.setName(LocalisedString.from(item.getName()));
            target.setValue(LocalisedString.from(item.getValue()));
            return target;
        }).collect(Collectors.toList());
    }
}
