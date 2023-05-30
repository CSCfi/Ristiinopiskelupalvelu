package fi.uta.ristiinopiskelu.handler.validator.realisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.validator.JsonRequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Validator;

@Component
public class UpdateRealisationValidator extends AbstractRealisationValidator implements JsonRequestValidator<RealisationEntity> {

    private ObjectMapper objectMapper;

    protected UpdateRealisationValidator() {
    }

    @Autowired
    public UpdateRealisationValidator(ModelMapper modelMapper,
                                      ObjectMapper objectMapper,
                                      RealisationService realisationService,
                                      CourseUnitService courseUnitService,
                                      NetworkService networkService, Validator beanValidator) {
        super(modelMapper, realisationService, courseUnitService, networkService, beanValidator);
        this.objectMapper = objectMapper;
    }

    @Override
    public RealisationEntity validateJson(JsonNode request, String organisationId) throws ValidationException {
        if(organisationId == null || organisationId.isEmpty()) {
            throw new InvalidMessageHeaderException("Cannot perform update. Organisation Id is missing from header. This should not happen.");
        }

        if(request.get("realisation") == null) {
            throw new InvalidMessageBodyException("Received update realisation request -message without realisation -object.");
        }

        JsonNode realisationJson = request.get("realisation");
        RealisationWriteDTO parsedRealisation = objectMapper.convertValue(realisationJson, RealisationWriteDTO.class);
        super.validateObject(parsedRealisation, organisationId);

        RealisationEntity originalRealisation = realisationService.findByIdAndOrganizingOrganisationId(
                parsedRealisation.getRealisationId(), organisationId).orElse(null);

        if(originalRealisation == null) {
            throw new EntityNotFoundException("Unable to update realisation. Realisation with " + getRealisationIdString(parsedRealisation) + " does not exists");
        }

        if(realisationJson.has("cooperationNetworks")) {
            validateGivenNetworks(parsedRealisation, organisationId);
        }

        // If update will change study element references or cooperation networks, validate references.
        // This is required for cooperation network changes to verify all references still contain at least one same network
        if(realisationJson.has("studyElementReferences") || realisationJson.has("cooperationNetworks")) {
            // If update message is not updating study element references, setup with original values
            if(!realisationJson.has("studyElementReferences")) {
                parsedRealisation.setStudyElementReferences(originalRealisation.getStudyElementReferences());
            }

            // If update message is not updating cooperation networks, setup with original values
            if(!realisationJson.has("cooperationNetworks")) {
                parsedRealisation.setCooperationNetworks(originalRealisation.getCooperationNetworks());
            }

            validateStudyElementReferences(parsedRealisation, organisationId);
        }

        return originalRealisation;
    }

    protected void validateGivenNetworks(RealisationWriteDTO parsedRealisation, String organisationId) throws ValidationException {
        super.validateGivenNetworks(parsedRealisation, organisationId, MessageType.UPDATE_REALISATION_REQUEST, false);
    }

    protected void validateStudyElementReferences(RealisationWriteDTO parsedRealisation, String organisationId) throws ValidationException {
        super.validateStudyElementReferences(parsedRealisation, organisationId, MessageType.UPDATE_REALISATION_REQUEST);
    }
}
