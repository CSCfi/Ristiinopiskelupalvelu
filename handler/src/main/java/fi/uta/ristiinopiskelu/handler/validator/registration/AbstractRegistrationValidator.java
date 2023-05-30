package fi.uta.ristiinopiskelu.handler.validator.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;

public abstract class AbstractRegistrationValidator {

    protected String getSelectionIdString(RegistrationSelection selection) {
        if(selection == null) {
            return null;
        }

        return "[SelectionItemId: " + selection.getSelectionItemId() +
                " type: " + selection.getSelectionItemType() + "]";
    }
}
