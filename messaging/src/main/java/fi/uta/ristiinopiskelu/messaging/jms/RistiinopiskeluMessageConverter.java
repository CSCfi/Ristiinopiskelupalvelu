package fi.uta.ristiinopiskelu.messaging.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class RistiinopiskeluMessageConverter extends MappingJackson2MessageConverter {

    private Map<String, Class<?>> typeIds;

    public RistiinopiskeluMessageConverter(ObjectMapper objectMapper, VersionedMessageType[] messageTypeValues) {
        Assert.notNull(objectMapper, "ObjectMapper cannot be null");
        Assert.notEmpty(messageTypeValues, "MessageType values cannot be empty");

        this.typeIds = Arrays.stream(messageTypeValues)
                .collect(Collectors.toMap(VersionedMessageType::name, VersionedMessageType::getClazz));

        setObjectMapper(objectMapper);
        setTypeIdPropertyName(MessageHeader.MESSAGE_TYPE);
        setTypeIdMappings(this.typeIds);
        setTargetType(org.springframework.jms.support.converter.MessageType.TEXT);
    }

    public void addTypeId(String typeId, Class<? extends fi.uta.ristiinopiskelu.messaging.message.Message> messageClass) {
        typeIds.put(typeId, messageClass);
        setTypeIdMappings(this.typeIds);
    }

    public boolean isConvertable(Object object) {
        return object != null && this.typeIds.values().stream().anyMatch(typeClass -> object.getClass().isAssignableFrom(typeClass));
    }

    public boolean isConvertable(Message message) throws JMSException {
        if(message == null) {
            return false;
        }

        String messageType = message.getStringProperty(MessageHeader.MESSAGE_TYPE);
        if(StringUtils.isEmpty(messageType)) {
            throw new MessageConversionException(
                "Could not find type id property [" + MessageHeader.MESSAGE_TYPE + "] on message [" +
                    message.getJMSMessageID() + "] from destination [" + message.getJMSDestination() + "]");
        }

        return this.typeIds.containsKey(messageType);
    }
}
