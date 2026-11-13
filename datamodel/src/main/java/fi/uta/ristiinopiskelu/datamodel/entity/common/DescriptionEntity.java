package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionEntity {

    private String key;
    private LocalisedStringEntity name;
    private LocalisedStringEntity value;

    public static DescriptionEntity from(Description source) {
        if (source == null) {
            return null;
        }
        DescriptionEntity target = new DescriptionEntity();
        target.setKey(source.getKey());
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setValue(LocalisedStringEntity.from(source.getValue()));
        return target;
    }

    public static List<DescriptionEntity> from(List<Description> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(DescriptionEntity::from).toList();
    }
}
