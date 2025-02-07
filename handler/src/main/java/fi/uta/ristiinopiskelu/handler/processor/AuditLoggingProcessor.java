package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import net.logstash.logback.marker.LogstashMarker;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static net.logstash.logback.marker.Markers.append;

@Component
public class AuditLoggingProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingProcessor.class);

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

        logger.info(
                markers,
                "Received message '{}' from user '{}' at organisation '{}'", messageTypeValue, eppn, organisationId);
    }
}
