package fi.uta.ristiinopiskelu.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties("general.message-schema.version")
public class MessageSchemaVersionConfig {

    private Map<Integer, Class<VersionedMessageType>> available = new HashMap<>();
    private int current;

    public Map<Integer, Class<VersionedMessageType>> getAvailable() {
        return available;
    }

    public void setAvailable(Map<Integer, Class<VersionedMessageType>> available) {
        this.available = available;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
