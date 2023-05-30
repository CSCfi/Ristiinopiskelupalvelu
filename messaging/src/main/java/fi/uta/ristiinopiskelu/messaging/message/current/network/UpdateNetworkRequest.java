package fi.uta.ristiinopiskelu.messaging.message.current.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public class UpdateNetworkRequest extends AbstractRequest {
    
    private NetworkWriteDTO network = null;

    public NetworkWriteDTO getNetwork() { return network; }

    public void setNetwork(NetworkWriteDTO network) { this.network = network; }
}
