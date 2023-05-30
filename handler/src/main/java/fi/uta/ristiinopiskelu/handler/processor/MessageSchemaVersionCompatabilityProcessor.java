package fi.uta.ristiinopiskelu.handler.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageSchemaVersionCompatabilityProcessor implements Processor {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageSchemaService messageSchemaService;

    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode json = objectMapper.readTree(exchange.getIn().getBody(String.class));
        MessageType messageType = MessageHeaderUtils.getMessageType(exchange.getIn().getHeaders());
        int schemaVersion = MessageHeaderUtils.getSchemaVersion(exchange.getIn().getHeaders());

        JsonNode convertedJson = messageSchemaService.convertJson(json,
                messageSchemaService.getMessageTypeForVersion(messageType.name(), schemaVersion).getClazz(),
                messageType.getClazz());

        // reset headers too along with the converted JSON
        exchange.getMessage().setHeaders(exchange.getIn().getHeaders());
        exchange.getMessage().setBody(objectMapper.writeValueAsString(convertedJson));
    }
}
