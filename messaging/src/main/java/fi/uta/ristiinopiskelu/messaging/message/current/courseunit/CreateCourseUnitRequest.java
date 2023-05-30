package fi.uta.ristiinopiskelu.messaging.message.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

import java.util.List;

public class CreateCourseUnitRequest extends AbstractRequest {
    
    private List<CourseUnitWriteDTO> courseUnits;

    public List<CourseUnitWriteDTO> getCourseUnits() {
        return courseUnits;
    }

    public void setCourseUnits(List<CourseUnitWriteDTO> courseUnits) {
        this.courseUnits = courseUnits;
    }
}
