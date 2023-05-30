package fi.uta.ristiinopiskelu.handler.helper;

import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Message;
import javax.jms.MessageConsumer;

public class JmsHelper {

    private static int messageSchemaVersion;

    public static Message sendAndReceiveJson(JmsTemplate jmsTemplate, String json, String messageTypeName, String organisationId) {
        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = session.createTextMessage(json);
            message.setStringProperty(MessageHeader.MESSAGE_TYPE, messageTypeName);
            message.setStringProperty(MessageHeader.JMS_XUSERID, organisationId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            message.setStringProperty(MessageHeader.SCHEMA_VERSION, String.valueOf(messageSchemaVersion));
            return message;
        });
    }

    public static Message sendAndReceiveJsonWithUserId(JmsTemplate jmsTemplate, String json, String messageTypeName, String userId) {
        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = session.createTextMessage(json);
            message.setStringProperty(MessageHeader.MESSAGE_TYPE, messageTypeName);
            message.setStringProperty(MessageHeader.JMS_XUSERID, userId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            message.setStringProperty(MessageHeader.SCHEMA_VERSION, String.valueOf(messageSchemaVersion));
            return message;
        });
    }

    public static Message sendAndReceiveObject(JmsTemplate jmsTemplate, fi.uta.ristiinopiskelu.messaging.message.Message req, String organisationId) {
        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = jmsTemplate.getMessageConverter().toMessage(req, session);
            message.setStringProperty(MessageHeader.JMS_XUSERID, organisationId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            return message;
        });
    }

    public static Message sendAndReceiveObjectWithUserId(JmsTemplate jmsTemplate, fi.uta.ristiinopiskelu.messaging.message.Message req, String userId) {
        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = jmsTemplate.getMessageConverter().toMessage(req, session);
            message.setStringProperty(MessageHeader.JMS_XUSERID, userId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            return message;
        });
    }

    public static Message sendAndReceiveAcknowledgement(JmsTemplate jmsTemplate, fi.uta.ristiinopiskelu.messaging.message.Message req, String organisationId, String correlationId) {
        return jmsTemplate.sendAndReceive("handler", session -> {
            Message message = jmsTemplate.getMessageConverter().toMessage(req, session);
            message.setStringProperty(MessageHeader.JMS_XUSERID, organisationId);
            message.setStringProperty(MessageHeader.EPPN, "teppo@tuni.fi");
            message.setStringProperty(MessageHeader.RIPA_CORRELATION_ID, correlationId);
            return message;
        });
    }

    public static Message receiveObject(JmsTemplate jmsTemplate, String queue) {
        return JmsHelper.receiveObject(jmsTemplate, queue, 1000);
    }

    public static Message receiveObject(JmsTemplate jmsTemplate, String queue, long timeout) {
        return jmsTemplate.execute(session -> {
            try (final MessageConsumer consumer = session.createConsumer(session.createQueue(queue))) {
                return consumer.receive(timeout);
            }
        }, true);
    }

    public static void setMessageSchemaVersion(int messageSchemaVersion) {
        JmsHelper.messageSchemaVersion = messageSchemaVersion;
    }
}
