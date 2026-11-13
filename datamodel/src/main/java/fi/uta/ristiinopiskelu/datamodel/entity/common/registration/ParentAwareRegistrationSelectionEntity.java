package fi.uta.ristiinopiskelu.datamodel.entity.common.registration;

public interface ParentAwareRegistrationSelectionEntity extends RegistrationSelectionEntity {

    CreateRegistrationSelectionParentEntity getParent();
}
