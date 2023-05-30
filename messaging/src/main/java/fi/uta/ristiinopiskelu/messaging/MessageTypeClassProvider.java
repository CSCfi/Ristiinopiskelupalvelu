package fi.uta.ristiinopiskelu.messaging;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for providing MessageTypes for different schema versions.
 *
 * IMPORTANT! This needs to be manually modified each time a major schema version bump occurs.
 */
@Component
public class MessageTypeClassProvider {

    private static final int CURRENT_VERSION = 9;
    private static final int PREVIOUS_VERSION = 8;

    // MessageTypes per schema version
    private final Map<Integer, VersionedMessageType[]> messageTypes = new HashMap<>();

    // MessageType overrides per version, when for example MessageType.name() changes to a different one between versions
    private final Map<Integer, Map<VersionedMessageType, VersionedMessageType>> messageTypeOverrides = new HashMap<>();

    // Adjust according to versions in use
    public MessageTypeClassProvider() {
        this.messageTypes.put(CURRENT_VERSION, MessageType.values());
        this.messageTypes.put(PREVIOUS_VERSION, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.values());

        Map<VersionedMessageType, VersionedMessageType> v8Overrides = new HashMap<>();
        v8Overrides.put(MessageType.NETWORK_CREATED_NOTIFICATION, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST);
        v8Overrides.put(MessageType.NETWORK_UPDATED_NOTIFICATION, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_NETWORK_REQUEST);
        this.messageTypeOverrides.put(PREVIOUS_VERSION, v8Overrides);
    }

    public VersionedMessageType getMessageTypeForVersion(String messageTypeName, int version) {
        if(!this.messageTypes.containsKey(version)) {
            throw new IllegalArgumentException("Unknown MessageType version " + version);
        }

        for(VersionedMessageType versionedMessageType : this.messageTypes.get(version)) {
            if(versionedMessageType.name().equals(messageTypeName)) {
                return versionedMessageType;
            }
        }

        if(!CollectionUtils.isEmpty(messageTypeOverrides)) {
            Map<VersionedMessageType, VersionedMessageType> overrides = messageTypeOverrides.get(version);
            if(!CollectionUtils.isEmpty(overrides)) {
                for(Entry<VersionedMessageType, VersionedMessageType> entry : overrides.entrySet()) {
                    if(entry.getKey().name().equals(messageTypeName)) {
                        return entry.getValue();
                    }
                }
            }
        }
        
        throw new IllegalArgumentException("No MessageType " + messageTypeName + " found for version " + version);
    }

    public Map<Integer, VersionedMessageType[]> getMessageTypes() {
        return messageTypes;
    }

    public Map<Integer, Map<VersionedMessageType, VersionedMessageType>> getMessageTypeOverrides() {
        return messageTypeOverrides;
    }

    public int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public int getPreviousVersion() {
        return PREVIOUS_VERSION;
    }
}
