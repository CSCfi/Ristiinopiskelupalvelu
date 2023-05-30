package fi.uta.ristiinopiskelu.messaging.message.v8.network;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network;

public class UpdateNetworkRequest extends AbstractRequest {
    
    private Network network = null;

    public Network getNetwork() { return network; }

    public void setNetwork(Network network) { this.network = network; }
}
