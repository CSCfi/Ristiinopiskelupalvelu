package fi.uta.ristiinopiskelu.datamodel.dto.current.search.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import java.util.Map;

public class RegistrationStatusAmountSearchResults {

    private Map<String, Map<RegistrationSelectionItemStatus, Long>> incoming;
    private Map<String, Map<RegistrationSelectionItemStatus, Long>> outgoing;

    public RegistrationStatusAmountSearchResults(Map<String, Map<RegistrationSelectionItemStatus, Long>> incoming, Map<String, Map<RegistrationSelectionItemStatus, Long>> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    public Map<String, Map<RegistrationSelectionItemStatus, Long>> getIncoming() {
        return incoming;
    }

    public void setIncoming(Map<String, Map<RegistrationSelectionItemStatus, Long>> incoming) {
        this.incoming = incoming;
    }

    public Map<String, Map<RegistrationSelectionItemStatus, Long>> getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(Map<String, Map<RegistrationSelectionItemStatus, Long>> outgoing) {
        this.outgoing = outgoing;
    }
}
