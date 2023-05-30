package fi.uta.ristiinopiskelu.messaging.message.v8.realisation;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;

public class UpdateRealisationRequest extends AbstractRequest {
    
    private Realisation realisation;

    public Realisation getRealisation() {
        return realisation;
    }

    public void setRealisation(Realisation realisation) {
        this.realisation = realisation;
    }
}
