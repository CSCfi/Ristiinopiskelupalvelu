package fi.uta.ristiinopiskelu.handler.jms;

import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import org.apache.camel.component.jms.JmsMessageType;

import java.util.Map;

public final class JmsMessageTypeResolver {

    private static final int LEGACY_BYTES_MESSAGE_SCHEMA_VERSION = 8;
    private static final int TEXT_ONLY_SCHEMA_VERSION = 10;

    private JmsMessageTypeResolver() {
    }

    public static JmsMessageType resolveForForwardedMessage(int schemaVersion) {
        return schemaVersion == LEGACY_BYTES_MESSAGE_SCHEMA_VERSION ? JmsMessageType.Bytes : JmsMessageType.Text;
    }

    public static JmsMessageType resolveForReply(Map<String, Object> headers) {
        try {
            int schemaVersion = MessageHeaderUtils.getSchemaVersion(headers);
            return schemaVersion >= TEXT_ONLY_SCHEMA_VERSION ? JmsMessageType.Text : JmsMessageType.Bytes;
        } catch (IllegalArgumentException e) {
            return JmsMessageType.Bytes;
        }
    }
}
