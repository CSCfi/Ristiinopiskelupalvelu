package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import org.springframework.stereotype.Component;

@Component
public class CreateCourseUnitRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest, CreateCourseUnitRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.courseunit.CreateCourseUnitRequest.class;
    }

    @Override
    public Class<CreateCourseUnitRequest> getTargetType() {
        return CreateCourseUnitRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("courseUnits", json);
    }
}
