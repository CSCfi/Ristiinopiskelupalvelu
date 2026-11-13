package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.exception.validation.UnallowedMessageTypeException;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProcessor.class);

    @Value("${spring.artemis.admin-ui-user}")
    private String adminUiUser;

    @Override
    public void process(Exchange exchange) throws Exception {
        String jmsxUserId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);
        String messageType = exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE, String.class);
        String organisationId;

        if(!StringUtils.hasText(jmsxUserId)) {
            throw new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);
        }

        if(!StringUtils.hasText(messageType)) {
            throw new MissingMessageHeaderException(MessageHeader.MESSAGE_TYPE);
        }

        try {
            MessageType parsedMessageType = MessageType.valueOf(messageType);
            MessageType.getMessageGroup(parsedMessageType);
        } catch (IllegalArgumentException e) {
            throw new UnallowedMessageTypeException("Unknown or unrouteable message type: " + messageType, e);
        }

        if(jmsxUserId.equals(adminUiUser)) {
            if(!MessageGroup.NETWORK.contains(messageType)) {
                throw new UnallowedMessageTypeException("Admin-ui user cannot exchange message type " + messageType + ", only Network message types.");
            } else {
                organisationId = adminUiUser;
            }
        } else {
            if(MessageGroup.NETWORK.contains(messageType)) {
                throw new UnallowedMessageTypeException("Organisation user cannot exchange message type " + messageType);
            }
            organisationId = jmsxUserId;

        }

        exchange.setMessage(exchange.getIn());
        exchange.getMessage().setHeader(MessageHeader.JMS_XUSERID, organisationId);
    }
}
