package fi.uta.ristiinopiskelu.messaging.schema.converter.v8tov9.request;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.messaging.exception.ObjectConversionException;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import org.springframework.stereotype.Component;

@Component
public class CreateRealisationRequestV8ToV9Converter extends AbstractStudyElementRequestV8ToV9Converter<
    fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest, CreateRealisationRequest> {

    @Override
    public Class<fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest> getSourceType() {
        return fi.uta.ristiinopiskelu.messaging.message.v8.realisation.CreateRealisationRequest.class;
    }

    @Override
    public Class<CreateRealisationRequest> getTargetType() {
        return CreateRealisationRequest.class;
    }

    @Override
    public JsonNode convertJson(JsonNode json) throws ObjectConversionException {
        return convertJson("realisations", json);
    }
}
