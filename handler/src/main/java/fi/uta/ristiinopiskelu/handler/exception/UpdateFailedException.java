package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class UpdateFailedException extends RistiinopiskeluException {

    public UpdateFailedException(String message) {
        super(message);
    }

    public <T extends GenericEntity> UpdateFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to update " + type.getSimpleName() + " with id " + id, cause);
    }

    public <T extends GenericEntity> UpdateFailedException(Class<T> type, String id, String code, Throwable cause) {
        super("Failed to update " + type.getSimpleName() + " with id " + id + " and code " + code, cause);
    }
}
