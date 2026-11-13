package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import fi.uta.ristiinopiskelu.handler.exception.RistiinopiskeluException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.apache.camel.Exchange;

public final class ErrorResponseMessageResolver {

    private static final String INTERNAL_ERROR_MESSAGE =
        "Message processing failed due to an internal error.";

    private ErrorResponseMessageResolver() {
    }

    public static String resolve(Exchange exchange, Exception exception) {
        String messageType = String.valueOf(exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE));
        RuntimeException safeException = findSafeException(exception);

        if (safeException != null) {
            return "Failed to process message with type %s, error: %s"
                .formatted(messageType, safeException.getMessage());
        }

        return "Failed to process message with type %s, error: %s"
            .formatted(messageType, INTERNAL_ERROR_MESSAGE);
    }

    public static ErrorCode resolveErrorCode(Exception exception) {
        RistiinopiskeluException clientException = findClientException(exception);
        if (clientException != null) {
            return clientException.getErrorCode();
        }

        Throwable current = exception;
        while (current != null) {
            if (current instanceof RistiinopiskeluException ristiinopiskeluException) {
                return ristiinopiskeluException.getErrorCode();
            }
            current = current.getCause();
        }

        return ErrorCode.INTERNAL_ERROR;
    }

    private static RuntimeException findSafeException(Throwable throwable) {
        RistiinopiskeluException clientException = findClientException(throwable);
        if (clientException != null) {
            return clientException;
        }

        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MessageForwardingFailedException messageForwardingFailedException) {
                return messageForwardingFailedException;
            }
            current = current.getCause();
        }

        return null;
    }

    private static RistiinopiskeluException findClientException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ValidationException validationException) {
                return validationException;
            }
            if (current instanceof InvalidSearchParametersException invalidSearchParametersException) {
                return invalidSearchParametersException;
            }
            current = current.getCause();
        }

        return null;
    }
}
