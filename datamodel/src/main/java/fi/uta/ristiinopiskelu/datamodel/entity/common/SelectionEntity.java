package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Selection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionEntity {

    private LocalisedStringEntity title;
    private SelectionTypeEntity type;
    private List<SelectionValueEntity> selectionValues;

    public static SelectionEntity from(Selection source) {
        if (source == null) {
            return null;
        }
        SelectionEntity target = new SelectionEntity();
        target.setTitle(LocalisedStringEntity.from(source.getTitle()));
        target.setType(source.getType() == null ? null : SelectionTypeEntity.valueOf(source.getType().name()));
        target.setSelectionValues(SelectionValueEntity.from(source.getSelectionValues()));
        return target;
    }

    public static List<SelectionEntity> from(List<Selection> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(SelectionEntity::from).toList();
    }
}
