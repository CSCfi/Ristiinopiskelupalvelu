package fi.uta.ristiinopiskelu.messaging.message.v8.realisation;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;

import java.util.List;

public class CreateRealisationRequest extends AbstractRequest {
    
    private List<Realisation> realisations;

    public List<Realisation> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<Realisation> realisations) {
        this.realisations = realisations;
    }
}
