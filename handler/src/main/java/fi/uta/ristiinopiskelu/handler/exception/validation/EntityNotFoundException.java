package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class EntityNotFoundException extends ValidationException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id) {
        super("Entity " + type.getSimpleName() + " not found with id '" + id + "'");
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String organizingOrganisation) {
        super("Entity " + type.getSimpleName() + " not found with id '" + id + "' and organisation id  '" + organizingOrganisation + "'");
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String code, String organizingOrganisation) {
        super("Entity " + type.getSimpleName() + " not found with id '"+ id + "' code '" + code + "' and organisation id  '" + organizingOrganisation + "'");
    }
}
