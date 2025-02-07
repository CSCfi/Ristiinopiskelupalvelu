package fi.uta.ristiinopiskelu.messaging.message.current.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class CreateRealisationRequest extends AbstractRequest {

    private List<RealisationWriteDTO> realisations;

    @ArraySchema(schema = @Schema(name = "realisations", implementation = RealisationWriteDTO.class))
    public List<RealisationWriteDTO> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<RealisationWriteDTO> realisations) {
        this.realisations = realisations;
    }
}
