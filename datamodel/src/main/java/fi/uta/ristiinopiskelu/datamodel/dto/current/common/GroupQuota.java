package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.GroupQuotaEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "GroupQuotaV9", title = "GroupQuotaV9")
public class GroupQuota {
    private List<String> networkIdentifiers;
    private Integer minSeats;
    private Integer maxSeats;

    public List<String> getNetworkIdentifiers() {
        return networkIdentifiers;
    }

    public void setNetworkIdentifiers(List<String> networkIdentifiers) {
        this.networkIdentifiers = networkIdentifiers;
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

    public static GroupQuota from(GroupQuotaEntity source) {
        if (source == null) {
            return null;
        }
        GroupQuota target = new GroupQuota();
        target.setNetworkIdentifiers(source.getNetworkIdentifiers());
        target.setMinSeats(source.getMinSeats());
        target.setMaxSeats(source.getMaxSeats());
        return target;
    }

    public static List<GroupQuota> from(List<GroupQuotaEntity> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(GroupQuota::from).toList();
    }
}
