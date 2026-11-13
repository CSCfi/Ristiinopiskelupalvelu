package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

public interface ParentAwareRegistrationSelection extends RegistrationSelection {

    CreateRegistrationSelectionParent getParent();
}
