package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CooperationNetworkEntity {

    private String id;
    private LocalisedStringEntity name;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityEndDate;

    private Boolean enrollable;

    public static CooperationNetworkEntity from(CooperationNetwork source) {
        if (source == null) {
            return null;
        }
        CooperationNetworkEntity target = new CooperationNetworkEntity();
        target.setId(source.getId());
        target.setName(LocalisedStringEntity.from(source.getName()));
        target.setValidityStartDate(source.getValidityStartDate());
        target.setValidityEndDate(source.getValidityEndDate());
        target.setEnrollable(source.getEnrollable());
        return target;
    }

    public static List<CooperationNetworkEntity> from(List<CooperationNetwork> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(CooperationNetworkEntity::from).toList();
    }
}
