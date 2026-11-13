package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Keyword;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeywordEntity {

    private String key;
    private LocalisedStringEntity value;
    private String keySet;
    private LocalisedStringEntity keySetValue;

    public static KeywordEntity from(Keyword source) {
        if (source == null) {
            return null;
        }
        KeywordEntity target = new KeywordEntity();
        target.setKey(source.getKey());
        target.setValue(LocalisedStringEntity.from(source.getValue()));
        target.setKeySet(source.getKeySet());
        target.setKeySetValue(LocalisedStringEntity.from(source.getKeySetValue()));
        return target;
    }

    public static List<KeywordEntity> from(List<Keyword> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(KeywordEntity::from).toList();
    }
}
