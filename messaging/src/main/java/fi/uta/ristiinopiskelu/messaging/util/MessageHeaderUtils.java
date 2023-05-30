package fi.uta.ristiinopiskelu.messaging.util;

import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.springframework.util.Assert;

import java.util.Map;

public class MessageHeaderUtils {

    public static int getSchemaVersion(Map<String, Object> headers) {
        Object schemaVersionValue = headers.get(MessageHeader.SCHEMA_VERSION);
        if(schemaVersionValue != null) {
            if (schemaVersionValue instanceof String) {
                String schemaVersionStringValue = (String) schemaVersionValue;
                Assert.hasText(schemaVersionStringValue, String.format("Schema version number missing, must be supplied in header with key '%s'", MessageHeader.SCHEMA_VERSION));
                return Integer.parseInt(schemaVersionStringValue);
            }

            if (schemaVersionValue instanceof Integer) {
                return (int) schemaVersionValue;
            }
        }

        throw new IllegalArgumentException(String.format("Schema version number missing, must be supplied in header with key '%s'", MessageHeader.SCHEMA_VERSION));
    }

    public static MessageType getMessageType(Map<String, Object> headers) {
        String messageTypeValue = (String) headers.get(MessageHeader.MESSAGE_TYPE);
        Assert.hasText(messageTypeValue, "Message type missing, must be supplied in header with key '" + MessageHeader.MESSAGE_TYPE + "'");
        return MessageType.valueOf(messageTypeValue);
    }

    public static String getOrganisationId(Map<String, Object> headers) {
        String organisationId = (String) headers.get(MessageHeader.JMS_XUSERID);
        Assert.hasText(organisationId, "Organisation id missing, unauthorized organisation/misconfiguration?");
        return organisationId;
    }
}
