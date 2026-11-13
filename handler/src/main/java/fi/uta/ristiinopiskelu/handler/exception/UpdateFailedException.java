package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class UpdateFailedException extends RistiinopiskeluException {

    public UpdateFailedException(String message) {
        super(message, ErrorCode.SERVICE_UPDATE_FAILED);
    }

    public <T extends GenericEntity> UpdateFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to update %s with id %s".formatted(type.getSimpleName(), id), cause, ErrorCode.SERVICE_UPDATE_FAILED);
    }

    public <T extends GenericEntity> UpdateFailedException(Class<T> type, String id, String code, Throwable cause) {
        super("Failed to update %s with id %s and code %s".formatted(type.getSimpleName(), id, code), cause, ErrorCode.SERVICE_UPDATE_FAILED);
    }
}
