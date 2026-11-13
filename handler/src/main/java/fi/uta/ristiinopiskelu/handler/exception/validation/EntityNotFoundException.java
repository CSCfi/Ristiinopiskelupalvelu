package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;

public class EntityNotFoundException extends ValidationException {

    public EntityNotFoundException(String message) {
        super(message, ErrorCode.ENTITY_NOT_FOUND);
    }

    protected EntityNotFoundException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id) {
        this(type, id, ErrorCode.ENTITY_NOT_FOUND);
    }

    protected <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, ErrorCode errorCode) {
        super("Entity %s not found with id '%s'".formatted(type.getSimpleName(), id), errorCode);
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String organizingOrganisation) {
        this(type, id, organizingOrganisation, ErrorCode.ENTITY_NOT_FOUND);
    }

    protected <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String organizingOrganisation, ErrorCode errorCode) {
        super("Entity %s not found with id '%s' and organisation id  '%s'".formatted(type.getSimpleName(), id, organizingOrganisation), errorCode);
    }

    public <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String code, String organizingOrganisation) {
        this(type, id, code, organizingOrganisation, ErrorCode.ENTITY_NOT_FOUND);
    }

    protected <T extends GenericEntity> EntityNotFoundException(Class<T> type, String id, String code, String organizingOrganisation, ErrorCode errorCode) {
        super("Entity %s not found with id '%s' code '%s' and organisation id  '%s'".formatted(type.getSimpleName(), id, code, organizingOrganisation), errorCode);
    }
}
