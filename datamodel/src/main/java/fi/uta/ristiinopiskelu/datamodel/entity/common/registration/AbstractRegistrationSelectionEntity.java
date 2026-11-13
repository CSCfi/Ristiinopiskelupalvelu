package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractRegistrationSelectionEntity implements RegistrationSelectionEntity {

    private String selectionItemId;
    private RegistrationSelectionItemTypeEntity selectionItemType;
}
