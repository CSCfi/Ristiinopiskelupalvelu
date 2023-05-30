package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.UpdateRealisationRequest;
import org.springframework.stereotype.Component;

@Component
public class UpdateRealisationRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.realisation.UpdateRealisationRequest, UpdateRealisationRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.realisation.UpdateRealisationRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.realisation.UpdateRealisationRequest.class;
    }

    @Override
    public Class<UpdateRealisationRequest> getTargetType() {
        return UpdateRealisationRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("realisation", json);
    }
}
