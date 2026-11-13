package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.degree;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DegreeV9", title = "DegreeV9", type = "object")
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

    public static DegreeReadDTO from(DegreeEntity source) {
        DegreeReadDTO target = new DegreeReadDTO();
        StudyElementEntity.copyStudyElementEntityToReadDto(source, target);
        target.setDuration(source.getDuration());
        return target;
    }
}
