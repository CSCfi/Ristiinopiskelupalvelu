package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class DegreeAlreadyExistsValidationException extends ValidationException {

    public DegreeAlreadyExistsValidationException(String message) {
        super(message, ErrorCode.DEGREE_ALREADY_EXISTS);
    }
}
