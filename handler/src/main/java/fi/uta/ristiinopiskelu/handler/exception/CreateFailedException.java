package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class CreateFailedException extends RistiinopiskeluException {

    public CreateFailedException(String message) {
        super(message, ErrorCode.SERVICE_CREATE_FAILED);
    }

    public <T extends GenericEntity> CreateFailedException(Class<T> type, Throwable cause) {
        super("Failed to create %s".formatted(type.getSimpleName()), cause, ErrorCode.SERVICE_CREATE_FAILED);
    }
}
