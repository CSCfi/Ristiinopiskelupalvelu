package fi.uta.ristiinopiskelu.messaging.message.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class CreateCourseUnitRequest extends AbstractRequest {
    
    private List<CourseUnitWriteDTO> courseUnits;

    @ArraySchema(schema = @Schema(name = "courseUnits", implementation = CourseUnitWriteDTO.class))
    public List<CourseUnitWriteDTO> getCourseUnits() {
        return courseUnits;
    }

    public void setCourseUnits(List<CourseUnitWriteDTO> courseUnits) {
        this.courseUnits = courseUnits;
    }
}
