package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

public interface StatusAwareRegistrationSelectionEntity extends RegistrationSelectionEntity {

    RegistrationSelectionItemStatusEntity getSelectionItemStatus();
}
