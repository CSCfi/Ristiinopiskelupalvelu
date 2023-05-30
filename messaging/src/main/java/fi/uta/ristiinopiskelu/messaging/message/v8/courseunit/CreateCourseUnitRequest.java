package fi.uta.ristiinopiskelu.messaging.message.v8.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;

import java.util.List;

public class CreateCourseUnitRequest extends AbstractRequest {
    
    private List<CreateCourseUnitRequestDTO> courseUnits;

    public List<CreateCourseUnitRequestDTO> getCourseUnits() {
        return courseUnits;
    }

    public void setCourseUnits(List<CreateCourseUnitRequestDTO> courseUnits) {
        this.courseUnits = courseUnits;
    }
}
