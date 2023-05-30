package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class CreateFailedException extends RistiinopiskeluException {

    public CreateFailedException(String message) {
        super(message);
    }

    public <T extends GenericEntity> CreateFailedException(Class<T> type, Throwable cause) {
        super("Failed to create " + type.getSimpleName(), cause);
    }
}
