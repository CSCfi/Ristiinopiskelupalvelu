package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.List;

public class FindFailedException extends RistiinopiskeluException {

    public FindFailedException(String message) {
        super(message, ErrorCode.SERVICE_FIND_FAILED);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, Throwable cause) {
        super("Failed to find %s".formatted(type.getSimpleName()), cause, ErrorCode.SERVICE_FIND_FAILED);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to find %s with id %s".formatted(type.getSimpleName(), id), cause, ErrorCode.SERVICE_FIND_FAILED);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, List<String> ids, Throwable cause) {
        super("Failed to find %s with ids [%s]".formatted(type.getSimpleName(), StringUtils.collectionToDelimitedString(ids, ",")), cause, ErrorCode.SERVICE_FIND_FAILED);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, String organizingOrganisation, Throwable cause) {
        super("Failed to find %s not found with code '%s' and organisation id  '%s'".formatted(type.getSimpleName(), id, organizingOrganisation), cause, ErrorCode.SERVICE_FIND_FAILED);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, String code, String organizingOrganisation, Throwable cause) {
        super("Failed to find %s with id %s and identifier code %s and organizingOrganisation %s".formatted(type.getSimpleName(), id, code, organizingOrganisation), cause, ErrorCode.SERVICE_FIND_FAILED);
    }
}
