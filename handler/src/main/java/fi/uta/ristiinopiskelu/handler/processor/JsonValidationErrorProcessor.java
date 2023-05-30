package fi.uta.ristiinopiskelu.handler.processor;

import com.networknt.schema.ValidationMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import org.apache.camel.Exchange;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonValidationErrorProcessor extends AbstractErrorProcessor {

    @Override
    public RistiinopiskeluMessage createErrorResponse(Exchange exchange, Exception exception) {
        JsonValidationException validationException = (JsonValidationException) exception;

        List<String> errors = new ArrayList<>();
        if(!CollectionUtils.isEmpty(validationException.getErrors())) {
            errors.addAll(validationException.getErrors().stream()
                .map(ValidationMessage::getMessage).collect(Collectors.toList()));
        }

        return new RistiinopiskeluMessage(exchange,
                MessageType.JSON_VALIDATION_FAILED_RESPONSE,
                new JsonValidationFailedResponse(validationException.getMessage(), errors));
    }
}
