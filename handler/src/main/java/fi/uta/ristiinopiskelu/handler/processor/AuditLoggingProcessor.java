package fi.uta.ristiinopiskelu.handler.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.PersonIdentifiableMessage;
import net.logstash.logback.marker.LogstashMarker;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static net.logstash.logback.marker.Markers.append;

@Component
public class AuditLoggingProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        String eppn = exchange.getIn().getHeader(MessageHeader.EPPN, String.class);
        Assert.hasText(eppn, "End user eduPersonPrincipalName missing, must be supplied in header with key '" + MessageHeader.EPPN + "'");

        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        Assert.hasText(organisationId, "Organisation id missing, unauthorized organisation/misconfiguration?");

        String messageTypeValue = exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE, String.class);
        Assert.hasText(messageTypeValue, "Message type missing, must be supplied in header with key '" + MessageHeader.MESSAGE_TYPE + "'");

        LogstashMarker markers = append("eppn", eppn)
                .and(append("organisationId", organisationId))
                .and(append("messageType", messageTypeValue));

        MessageType messageType = MessageType.valueOf(messageTypeValue);

        if(PersonIdentifiableMessage.class.isAssignableFrom(messageType.getClazz())) {
            Message req = objectMapper.readValue(exchange.getIn().getBody(String.class), messageType.getClazz());
            PersonIdentifiableMessage personIdentifiableMessage = (PersonIdentifiableMessage) req;
            markers.and(append("targetPerson", personIdentifiableMessage.getPersonIdentifier()));

            logger.info(
                    markers,
                    "Received message '{}' from user '{}' at organisation '{}', target person '{}'", messageTypeValue, eppn,
                        organisationId, personIdentifiableMessage.getPersonIdentifier());
        } else {
            logger.info(
                    markers,
                    "Received message '{}' from user '{}' at organisation '{}'", messageTypeValue, eppn, organisationId);
        }
    }
}
