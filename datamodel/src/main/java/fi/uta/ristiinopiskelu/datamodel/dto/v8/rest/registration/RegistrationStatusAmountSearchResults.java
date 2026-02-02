package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "RegistrationStatusAmountSearchResultsV8", title = "RegistrationStatusAmountSearchResultsV8")
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
