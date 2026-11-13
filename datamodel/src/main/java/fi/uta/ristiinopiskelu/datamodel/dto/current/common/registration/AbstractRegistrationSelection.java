package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class AbstractRegistrationSelection implements RegistrationSelection {

    private String selectionItemId;
    private RegistrationSelectionItemType selectionItemType;
}
