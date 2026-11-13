package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelection;
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
public class CreateRegistrationSelectionEntity extends AbstractRegistrationSelectionEntity
    implements ParentAwareRegistrationSelectionEntity, StatusAwareRegistrationSelectionEntity {

    @Field(type = FieldType.Integer)
    private RegistrationSelectionItemStatusEntity selectionItemStatus;

    private String selectionItemStatusInfo;
    private CreateRegistrationSelectionParentEntity parent;
    private List<String> subGroupSelections;

    public static CreateRegistrationSelectionEntity from(CreateRegistrationSelection source) {
        if (source == null) {
            return null;
        }

        CreateRegistrationSelectionEntity target = new CreateRegistrationSelectionEntity();
        target.setSelectionItemId(source.getSelectionItemId());
        target.setSelectionItemType(RegistrationSelectionItemTypeEntity.from(source.getSelectionItemType()));
        target.setSelectionItemStatus(RegistrationSelectionItemStatusEntity.from(source.getSelectionItemStatus()));
        target.setSelectionItemStatusInfo(source.getSelectionItemStatusInfo());
        target.setParent(CreateRegistrationSelectionParentEntity.from(source.getParent()));
        target.setSubGroupSelections(source.getSubGroupSelections());
        return target;
    }

    public static List<CreateRegistrationSelectionEntity> from(List<CreateRegistrationSelection> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
            .map(CreateRegistrationSelectionEntity::from)
            .toList();
    }
}
