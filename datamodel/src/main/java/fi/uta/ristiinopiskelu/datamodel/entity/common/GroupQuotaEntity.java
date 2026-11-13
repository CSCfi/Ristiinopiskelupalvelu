package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.GroupQuota;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupQuotaEntity {

    private List<String> networkIdentifiers;
    private Integer minSeats;
    private Integer maxSeats;

    public static GroupQuotaEntity from(GroupQuota source) {
        if (source == null) {
            return null;
        }
        GroupQuotaEntity target = new GroupQuotaEntity();
        target.setNetworkIdentifiers(source.getNetworkIdentifiers());
        target.setMinSeats(source.getMinSeats());
        target.setMaxSeats(source.getMaxSeats());
        return target;
    }

    public static List<GroupQuotaEntity> from(List<GroupQuota> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(GroupQuotaEntity::from).toList();
    }
}
