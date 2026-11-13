package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.SelectionValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionValueEntity {

    private Integer minSeats;
    private Integer maxSeats;
    private LocalisedStringEntity name;
    private String id;
    private List<GroupQuotaEntity> groupQuotas;

    public static SelectionValueEntity from(SelectionValue source) {
        if (source == null) {
            return null;
        }
        SelectionValueEntity target = new SelectionValueEntity();
        target.setMinSeats(source.getMinSeats());
        target.setMaxSeats(source.getMaxSeats());
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setId(source.getId());
        target.setGroupQuotas(GroupQuotaEntity.from(source.getGroupQuotas()));
        return target;
    }

    public static List<SelectionValueEntity> from(List<SelectionValue> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(SelectionValueEntity::from).toList();
    }
}
