package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.CreateRegistrationSelectionParentEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRegistrationSelectionParent extends AbstractRegistrationSelection implements ParentAwareRegistrationSelection {

    // hidden for now, otherwise it will cause a stackoverflow due to an endless loop in springwolf
    @Schema(hidden = true)
    private CreateRegistrationSelectionParent parent;

    @Builder
    public CreateRegistrationSelectionParent(String selectionItemId, RegistrationSelectionItemType selectionItemType,
                                             CreateRegistrationSelectionParent parent) {
        super(selectionItemId, selectionItemType);
        this.parent = parent;
    }

    public static CreateRegistrationSelectionParent from(CreateRegistrationSelectionParentEntity source) {
        if (source == null) {
            return null;
        }

        return CreateRegistrationSelectionParent.builder()
            .selectionItemId(source.getSelectionItemId())
            .selectionItemType(RegistrationSelectionItemType.from(source.getSelectionItemType()))
            .parent(CreateRegistrationSelectionParent.from(source.getParent()))
            .build();
    }
}
