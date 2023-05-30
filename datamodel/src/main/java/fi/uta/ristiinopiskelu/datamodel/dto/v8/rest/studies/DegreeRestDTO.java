package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElementType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Degree")
public class DegreeRestDTO extends StudyElementRestDTO {

    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public DegreeRestDTO() {
        setType(StudyElementType.DEGREE);
    }

    public DegreeRestDTO(double duration) {
        this();
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }
}
