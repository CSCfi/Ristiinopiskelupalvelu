package fi.uta.ristiinopiskelu.messaging;

import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Class for providing MessageTypes for different schema versions.
 *
 * IMPORTANT! Versions need to be defined in application.yml. Overrides can be manually added in afterPropertiesSet().
 */
@Component
public class MessageTypeClassProvider implements InitializingBean {

    @Autowired
    private MessageSchemaVersionConfig messageSchemaVersionConfig;

    // MessageTypes per schema version
    private Map<Integer, VersionedMessageType[]> messageTypes = new HashMap<>();
    private List<Integer> availableVersions = new ArrayList<>();

    // MessageType overrides per version, when for example MessageType.name() changes to a different one between versions
    private final Map<Integer, Map<VersionedMessageType, VersionedMessageType>> messageTypeOverrides = new HashMap<>();

    public List<Integer> getAvailableVersions() {
        return availableVersions;
    }

    public boolean hasMessageTypeForVersion(int version) {
        return messageTypes.containsKey(version);
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
        return messageSchemaVersionConfig.getCurrent();
    }

    @Override
    public void afterPropertiesSet() {
        this.messageTypes = messageSchemaVersionConfig.getAvailable().entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getEnumConstants()));
        this.availableVersions = new ArrayList<>(messageTypes.keySet()).stream()
            .sorted()
            .toList();

        Map<VersionedMessageType, VersionedMessageType> v8Overrides = new HashMap<>();
        v8Overrides.put(MessageType.NETWORK_CREATED_NOTIFICATION, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.CREATE_NETWORK_REQUEST);
        v8Overrides.put(MessageType.NETWORK_UPDATED_NOTIFICATION, fi.uta.ristiinopiskelu.messaging.message.v8.MessageType.UPDATE_NETWORK_REQUEST);
        this.messageTypeOverrides.put(8, v8Overrides);
    }
}
