package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class NoForwardingOrganisationsValidationException extends ValidationException {
    public NoForwardingOrganisationsValidationException(String message) {
        super(message, ErrorCode.NO_FORWARDING_ORGANISATIONS);
    }

    public NoForwardingOrganisationsValidationException(String message, Throwable cause) {
        super(message, cause, ErrorCode.NO_FORWARDING_ORGANISATIONS);
    }
}
