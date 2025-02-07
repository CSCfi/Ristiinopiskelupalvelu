package fi.uta.ristiinopiskelu.messaging.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.MessageTypeClassProvider;
import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Message converter that delegates to an appropriate RistiinopiskeluConverter implementation based on the Object schema version.
 *
 * Note: toMessage(Object, Session) also sets MessageHeader.SCHEMA_VERSION automagically on all converted messages.
 * @see RistiinopiskeluMessageConverter
 */
public class VersionedMessageTypeAwareDelegatingMessageConverter implements MessageConverter {

    private static final Logger logger = LoggerFactory.getLogger(VersionedMessageTypeAwareDelegatingMessageConverter.class);

    private final int currentVersion;
    private Map<Integer, RistiinopiskeluMessageConverter> convertersByVersion = new HashMap<>();

    public VersionedMessageTypeAwareDelegatingMessageConverter(ObjectMapper objectMapper, MessageTypeClassProvider messageTypeClassProvider) {
        Assert.notNull(objectMapper, "ObjectMapper cannot be null");
        Assert.notNull(messageTypeClassProvider, "MessageTypeClassProvider cannot be null");

        this.currentVersion = messageTypeClassProvider.getCurrentVersion();

        Map<Integer, VersionedMessageType[]> messageTypes = messageTypeClassProvider.getMessageTypes();
        Assert.notEmpty(messageTypes, "MessageTypes cannot be empty, misconfiguration?");

        messageTypes.entrySet().stream().forEach(entry -> {
            int schemaVersion = entry.getKey();
            VersionedMessageType[] schemaMessageTypes = entry.getValue();

            RistiinopiskeluMessageConverter converter = new RistiinopiskeluMessageConverter(objectMapper, schemaMessageTypes);

            if(!CollectionUtils.isEmpty(messageTypeClassProvider.getMessageTypeOverrides())) {
                Map<VersionedMessageType, VersionedMessageType> overrides = messageTypeClassProvider.getMessageTypeOverrides().get(schemaVersion);
                if(!CollectionUtils.isEmpty(overrides)) {
                    for (Entry<VersionedMessageType, VersionedMessageType> override : overrides.entrySet()) {
                        converter.addTypeId(override.getKey().name(), override.getValue().getClazz());
                    }
                }
            }

            this.convertersByVersion.put(schemaVersion, converter);
        });
    }

    @Override
    public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
        Assert.notNull(object, "Object cannot be null");

        for(Entry<Integer, RistiinopiskeluMessageConverter> entry : this.convertersByVersion.entrySet()) {
            int schemaVersion = entry.getKey();
            RistiinopiskeluMessageConverter converter = entry.getValue();

            if(converter.isConvertable(object)) {
                Message message = converter.toMessage(object, session);
                message.setStringProperty(MessageHeader.SCHEMA_VERSION, String.valueOf(schemaVersion));
                return message;
            }
        }

        throw new MessageConversionException("Unable to convert object of type " + object.getClass() + ", no suitable converter found");
    }

    @Override
    public Object fromMessage(Message message) throws JMSException, MessageConversionException {
        Assert.notNull(message, "Message cannot be null");

        String schemaVersionValue = message.getStringProperty(MessageHeader.SCHEMA_VERSION);
        Integer schemaVersion = null;

        if(StringUtils.hasText(schemaVersionValue)) {
            try {
                schemaVersion = Integer.valueOf(schemaVersionValue);
            } catch (NumberFormatException e) {
                logger.error("Unable to parse schema version ['{}']", schemaVersionValue);
            }
        } else {
            logger.error("No schema version property [{}] found on message", MessageHeader.SCHEMA_VERSION);
        }

        if(schemaVersion == null) {
            schemaVersion = currentVersion;
        }

        RistiinopiskeluMessageConverter converter = this.convertersByVersion.get(schemaVersion);

        if(converter.isConvertable(message)) {
            return this.convertersByVersion.get(schemaVersion).fromMessage(message);
        }

        throw new MessageConversionException("Unable to convert message of type " + message.getClass() + ", no suitable converter found");
    }
}
