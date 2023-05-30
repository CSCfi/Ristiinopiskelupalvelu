package fi.uta.ristiinopiskelu.handler.validator;

import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public interface RequestValidator<T extends AbstractRequest> {

    void validateRequest(T message, String organisationId) throws ValidationException;
}
