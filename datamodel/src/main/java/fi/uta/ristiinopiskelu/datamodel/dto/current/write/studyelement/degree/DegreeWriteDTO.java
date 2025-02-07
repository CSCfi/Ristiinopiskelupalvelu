package fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Degree", allOf = { AbstractStudyElementWriteDTO.class })
public class DegreeWriteDTO extends AbstractStudyElementWriteDTO {

    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public DegreeWriteDTO() {
    }

    public DegreeWriteDTO(double duration) {
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }
}
