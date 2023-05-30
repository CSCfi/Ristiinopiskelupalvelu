package fi.uta.ristiinopiskelu.messaging.message.current.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.Notification;

import java.time.OffsetDateTime;

public class NetworkCreatedNotification implements Notification {

    private OffsetDateTime timestamp;
    private NetworkWriteDTO network = null;

    public NetworkCreatedNotification() {
    }

    public NetworkCreatedNotification(OffsetDateTime timestamp, NetworkWriteDTO network) {
        this.timestamp = timestamp;
        this.network = network;
    }

    public NetworkWriteDTO getNetwork() { return network; }

    public void setNetwork(NetworkWriteDTO network) { this.network = network; }

    @JsonIgnore
    @Override
    public String getSendingOrganisationTkCode() {
        return null;
    }

    @Override
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
