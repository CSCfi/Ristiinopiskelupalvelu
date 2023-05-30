package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import java.util.List;

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
}
