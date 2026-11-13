package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

public interface StatusAwareRegistrationSelection extends RegistrationSelection {

    RegistrationSelectionItemStatus getSelectionItemStatus();
}
