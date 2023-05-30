package fi.uta.ristiinopiskelu.messaging.message.v8.realisation;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

public class DeleteRealisationRequest extends AbstractRequest {

    private String realisationId;

    public String getRealisationId() {
        return realisationId;
    }

    public void setRealisationId(String realisationId) {
        this.realisationId = realisationId;
    }

}
