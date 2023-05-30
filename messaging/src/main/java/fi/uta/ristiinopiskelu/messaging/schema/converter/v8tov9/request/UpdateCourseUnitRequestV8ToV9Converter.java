package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.UpdateCourseUnitRequest;
import org.springframework.stereotype.Component;

@Component
public class UpdateCourseUnitRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.UpdateCourseUnitRequest, UpdateCourseUnitRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.UpdateCourseUnitRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.UpdateCourseUnitRequest.class;
    }

    @Override
    public Class<UpdateCourseUnitRequest> getTargetType() {
        return UpdateCourseUnitRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("courseUnit", json);
    }
}
