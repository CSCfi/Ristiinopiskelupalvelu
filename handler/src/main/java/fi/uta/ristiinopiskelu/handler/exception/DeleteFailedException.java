package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class DeleteFailedException extends RistiinopiskeluException {

    public DeleteFailedException(String message) {
        super(message, ErrorCode.SERVICE_DELETE_FAILED);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, Throwable cause) {
        super("Failed to delete %s".formatted(type.getSimpleName()), cause, ErrorCode.SERVICE_DELETE_FAILED);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to delete %s with id %s".formatted(type.getSimpleName(), id), cause, ErrorCode.SERVICE_DELETE_FAILED);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, String organizingOrganisation, Throwable cause) {
        super("Failed to delete %s with id %s and organizingOrganisation %s".formatted(type.getSimpleName(), id, organizingOrganisation), cause, ErrorCode.SERVICE_DELETE_FAILED);
    }

    public <T extends GenericEntity> DeleteFailedException(Class<T> type, String id, String code, String organizingOrganisation, Throwable cause) {
        super("Failed to delete %s with id %s and identifier code %s and organizingOrganisation %s".formatted(type.getSimpleName(), id, code, organizingOrganisation), cause, ErrorCode.SERVICE_DELETE_FAILED);
    }
}
