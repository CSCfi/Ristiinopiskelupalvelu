package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import org.springframework.util.StringUtils;

import java.util.List;

public class FindFailedException extends RistiinopiskeluException {

    public FindFailedException(String message) {
        super(message);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, Throwable cause) {
        super("Failed to find " + type.getSimpleName(), cause);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, Throwable cause) {
        super("Failed to find " + type.getSimpleName() + " with id " + id, cause);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, List<String> ids, Throwable cause) {
        super("Failed to find " + type.getSimpleName() + " with ids [" + StringUtils.collectionToDelimitedString(ids, ",") + "]", cause);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, String organizingOrganisation, Throwable cause) {
        super("Failed to find " + type.getSimpleName() + " not found with code '" + id + "' and organisation id  '" + organizingOrganisation + "'", cause);
    }

    public <T extends GenericEntity> FindFailedException(Class<T> type, String id, String code, String organizingOrganisation, Throwable cause) {
        super("Failed to find " + type.getSimpleName() + " with id " + id + " and identifier code " + code + " and organizingOrganisation " + organizingOrganisation, cause);
    }
}
