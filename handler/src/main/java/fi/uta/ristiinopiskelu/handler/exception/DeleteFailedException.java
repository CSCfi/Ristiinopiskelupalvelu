package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class DeleteFailedException extends RistiinopiskeluException {

    public DeleteFailedException(String message) {
        super(message);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, Throwable cause) {
        super("Failed to delete " + type.getSimpleName(), cause);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to delete " + type.getSimpleName() + " with id " + id, cause);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, String organizingOrganisation, Throwable cause) {
        super("Failed to delete " + type.getSimpleName() + " with id " + id + " and organizingOrganisation " + organizingOrganisation, cause);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, String code, String organizingOrganisation, Throwable cause) {
        super("Failed to delete " + type.getSimpleName() + " with id " + id + " and identifier code " + code + " and organizingOrganisation " + organizingOrganisation, cause);
    }
}
