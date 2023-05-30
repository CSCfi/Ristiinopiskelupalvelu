package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Degree")
public class DegreeReadDTO extends AbstractStudyElementReadDTO {

    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public DegreeReadDTO() {
        setType(StudyElementType.DEGREE);
    }

    public DegreeReadDTO(double duration) {
        this();
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }
}
