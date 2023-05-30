package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.UpdateStudyModuleRequest;
import org.springframework.stereotype.Component;

@Component
public class UpdateStudyModuleRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.UpdateStudyModuleRequest, UpdateStudyModuleRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.UpdateStudyModuleRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.studymodule.UpdateStudyModuleRequest.class;
    }

    @Override
    public Class<UpdateStudyModuleRequest> getTargetType() {
        return UpdateStudyModuleRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("studyModule", json);
    }
}
