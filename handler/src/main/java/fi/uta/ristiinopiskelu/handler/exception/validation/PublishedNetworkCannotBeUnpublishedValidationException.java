package fi.uta.ristiinopiskelu.handler.exception.validation;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;

public class PublishedNetworkCannotBeUnpublishedValidationException extends ValidationException {

    public PublishedNetworkCannotBeUnpublishedValidationException(String message) {
        super(message, ErrorCode.PUBLISHED_NETWORK_CANNOT_BE_UNPUBLISHED);
    }
}
