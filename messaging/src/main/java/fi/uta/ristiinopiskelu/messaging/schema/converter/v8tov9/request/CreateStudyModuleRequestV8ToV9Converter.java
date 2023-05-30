package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.CreateStudyModuleRequest;
import org.springframework.stereotype.Component;

@Component
public class CreateStudyModuleRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest, CreateStudyModuleRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.CreateStudyModuleRequest.class;
    }

    @Override
    public Class<CreateStudyModuleRequest> getTargetType() {
        return CreateStudyModuleRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("studyModules", json);
    }
}
