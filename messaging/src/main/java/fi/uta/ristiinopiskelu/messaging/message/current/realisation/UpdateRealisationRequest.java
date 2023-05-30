package fi.uta.ristiinopiskelu.messaging.message.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public class UpdateRealisationRequest extends AbstractRequest {
    
    private RealisationWriteDTO realisation;

    public RealisationWriteDTO getRealisation() {
        return realisation;
    }

    public void setRealisation(RealisationWriteDTO realisation) {
        this.realisation = realisation;
    }
}
