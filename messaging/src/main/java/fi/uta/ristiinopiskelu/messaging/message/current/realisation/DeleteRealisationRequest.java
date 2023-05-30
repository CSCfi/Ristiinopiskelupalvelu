package fi.uta.ristiinopiskelu.messaging.message.current.realisation;

import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public class DeleteRealisationRequest extends AbstractRequest {

    private String realisationId;

    public String getRealisationId() {
        return realisationId;
    }

    public void setRealisationId(String realisationId) {
        this.realisationId = realisationId;
    }

}
