package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationReplySelection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegistrationReplySelectionEntity extends AbstractRegistrationSelectionEntity implements StatusAwareRegistrationSelectionEntity {

    @Field(type = FieldType.Integer)
    private RegistrationSelectionItemStatusEntity selectionItemStatus;

    private String selectionItemStatusInfo;
    private List<String> subGroupSelections;
    private RankEntity rank;

    public static RegistrationReplySelectionEntity from(RegistrationReplySelection source) {
        if (source == null) {
            return null;
        }

        RegistrationReplySelectionEntity target = new RegistrationReplySelectionEntity();
        target.setSelectionItemId(source.getSelectionItemId());
        target.setSelectionItemType(RegistrationSelectionItemTypeEntity.from(source.getSelectionItemType()));
        target.setSelectionItemStatus(RegistrationSelectionItemStatusEntity.from(source.getSelectionItemStatus()));
        target.setSelectionItemStatusInfo(source.getSelectionItemStatusInfo());
        target.setSubGroupSelections(source.getSubGroupSelections());
        target.setRank(RankEntity.from(source.getRank()));
        return target;
    }

    public static List<RegistrationReplySelectionEntity> from(List<RegistrationReplySelection> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(RegistrationReplySelectionEntity::from)
            .toList();
    }
}
