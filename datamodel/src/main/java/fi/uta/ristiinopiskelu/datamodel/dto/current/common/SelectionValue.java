package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.SelectionValueEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SelectionValueV9", title = "SelectionValueV9")
public class SelectionValue {

    private Integer minSeats;
    private Integer maxSeats;
    private LocalisedString name;
    private String id;
    private List<GroupQuota> groupQuotas;

    public SelectionValue() {
        
    }

    public SelectionValue(Integer minSeats, Integer maxSeats, LocalisedString name, String id, List<GroupQuota> groupQuotas) {
        this.minSeats = minSeats;
        this.maxSeats = maxSeats;
        this.name = name;
        this.id = id;
        this.groupQuotas = groupQuotas;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<GroupQuota> getGroupQuotas() {
        return groupQuotas;
    }

    public void setGroupQuotas(List<GroupQuota> groupQuotas) {
        this.groupQuotas = groupQuotas;
    }

    public static SelectionValue from(SelectionValueEntity source) {
        if (source == null) {
            return null;
        }
        SelectionValue target = new SelectionValue();
        target.setMinSeats(source.getMinSeats());
        target.setMaxSeats(source.getMaxSeats());
        target.setName(LocalisedString.from(source.getName()));
        target.setId(source.getId());
        target.setGroupQuotas(GroupQuota.from(source.getGroupQuotas()));
        return target;
    }

    public static List<SelectionValue> from(List<SelectionValueEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(SelectionValue::from).toList();
    }
}
