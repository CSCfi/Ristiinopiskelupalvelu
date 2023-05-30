package fi.uta.ristiinopiskelu.messaging.message.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.messaging.message.current.AbstractRequest;

public class UpdateCourseUnitRequest extends AbstractRequest {
    
    private CourseUnitWriteDTO courseUnit;

    public CourseUnitWriteDTO getCourseUnit() {
        return courseUnit;
    }

    public void setCourseUnit(CourseUnitWriteDTO courseUnit) {
        this.courseUnit = courseUnit;
    }
}
