package fi.uta.ristiinopiskelu.messaging.message.v8.courseunit;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.CourseUnit;

public class UpdateCourseUnitRequest extends AbstractRequest {
    
    private CourseUnit courseUnit;

    public CourseUnit getCourseUnit() {
        return courseUnit;
    }

    public void setCourseUnit(CourseUnit courseUnit) {
        this.courseUnit = courseUnit;
    }
}
