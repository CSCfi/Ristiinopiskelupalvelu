package fi.uta.ristiinopiskelu.handler.validator;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.entity.GenericEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;

public interface JsonRequestValidator<E extends GenericEntity> {

    E validateJson(JsonNode json, String organisationId) throws ValidationException;
}
