package fi.uta.ristiinopiskelu.messaging.message.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

import java.util.List;

public class CreateRealisationRequest extends AbstractRequest {
    
    private List<RealisationWriteDTO> realisations;

    public List<RealisationWriteDTO> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<RealisationWriteDTO> realisations) {
        this.realisations = realisations;
    }
}
