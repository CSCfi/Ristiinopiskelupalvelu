package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelectionParent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateRegistrationSelectionParentEntity extends AbstractRegistrationSelectionEntity implements ParentAwareRegistrationSelectionEntity {

    private CreateRegistrationSelectionParentEntity parent;

    public static CreateRegistrationSelectionParentEntity from(CreateRegistrationSelectionParent source) {
        if (source == null) {
            return null;
        }

        CreateRegistrationSelectionParentEntity target = new CreateRegistrationSelectionParentEntity();
        target.setSelectionItemId(source.getSelectionItemId());
        target.setSelectionItemType(RegistrationSelectionItemTypeEntity.from(source.getSelectionItemType()));
        target.setParent(CreateRegistrationSelectionParentEntity.from(source.getParent()));
        return target;
    }
}
