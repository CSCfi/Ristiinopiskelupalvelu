package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class StudyModuleAlreadyExistsValidationException extends ValidationException {

    public StudyModuleAlreadyExistsValidationException(String message) {
        super(message, ErrorCode.STUDY_MODULE_ALREADY_EXISTS);
    }
}
