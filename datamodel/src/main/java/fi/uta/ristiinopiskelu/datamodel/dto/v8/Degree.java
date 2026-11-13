package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateDegreeRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyElementRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DegreeV8", title = "DegreeV8", type = "object")
public class Degree extends StudyElement {
    private double duration;

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getDuration() {
        return this.duration;
    }

    public Degree() {
        setType(StudyElementType.DEGREE);
    }

    public Degree(double duration) {
        this();
        this.duration = duration;
    }

    public String toString() {
        return "Degree(duration=" + this.getDuration() + ")";
    }

    public static Degree from(CreateDegreeRequestDTO source) {
        if (source == null) {
            return null;
        }

        Degree target = new Degree();
        CreateStudyElementRequestDTO.copyStudyElementFields(source, target);
        return target;
    }
}
