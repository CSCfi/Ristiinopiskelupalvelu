package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultMessage;
import org.springframework.util.Assert;
import java.util.Map;

public class RistiinopiskeluMessage extends DefaultMessage {

    public RistiinopiskeluMessage(Exchange exchange, MessageType messageType, Message messageBody) {
        super(exchange.getContext());
        Assert.notNull(messageType, "Message type cannot be null");
        Assert.notNull(messageBody, "Message body cannot be null");
        setHeader(MessageHeader.MESSAGE_TYPE, messageType.name());

        Integer schemaVersion = getSchemaVersion(exchange.getIn().getHeaders());
        if(schemaVersion != null) {
            setHeader(MessageHeader.SCHEMA_VERSION, schemaVersion);
        }

        setBody(messageBody);
    }

    public RistiinopiskeluMessage(Exchange exchange, MessageType messageType, String correlationId, Message messageBody) {
        super(exchange.getContext());
        Assert.notNull(messageType, "Message type cannot be null");
        Assert.notNull(messageBody, "Message body cannot be null");
        setHeader(MessageHeader.MESSAGE_TYPE, messageType.name());
        setHeader(MessageHeader.RIPA_CORRELATION_ID, correlationId);

        Integer schemaVersion = getSchemaVersion(exchange.getIn().getHeaders());
        if(schemaVersion != null) {
            setHeader(MessageHeader.SCHEMA_VERSION, schemaVersion);
        }

        setBody(messageBody);
    }

    private Integer getSchemaVersion(Map<String, Object> headers) {
        try {
            return MessageHeaderUtils.getSchemaVersion(headers);
        } catch (Exception e) {
            return null;
        }
    }
}
