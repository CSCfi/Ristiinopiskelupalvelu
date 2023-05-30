package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.v8.MessageType;
import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultMessage;
import org.springframework.util.Assert;

public class RistiinopiskeluMessage extends DefaultMessage {

    public RistiinopiskeluMessage(Exchange exchange, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType messageType, Message messageBody) {
        super(exchange.getContext());
        Assert.notNull(messageType, "Message type cannot be null");
        Assert.notNull(messageBody, "Message body cannot be null");
        setHeader(MessageHeader.MESSAGE_TYPE, messageType.name());
        setHeader(MessageHeader.SCHEMA_VERSION, MessageHeaderUtils.getSchemaVersion(exchange.getIn().getHeaders()));
        setBody(messageBody);
    }
    public RistiinopiskeluMessage(Exchange exchange, MessageType messageType, String correlationId, Message messageBody) {
        super(exchange.getContext());
        Assert.notNull(messageType, "Message type cannot be null");
        Assert.notNull(messageBody, "Message body cannot be null");
        setHeader(MessageHeader.MESSAGE_TYPE, messageType.name());
        setHeader(MessageHeader.SCHEMA_VERSION, MessageHeaderUtils.getSchemaVersion(exchange.getIn().getHeaders()));
        setHeader(MessageHeader.RIPA_CORRELATION_ID, correlationId);
        setBody(messageBody);
    }
}
