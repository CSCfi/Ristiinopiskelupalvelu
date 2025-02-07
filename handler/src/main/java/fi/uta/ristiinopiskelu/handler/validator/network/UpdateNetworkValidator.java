package fi.uta.ristiinopiskelu.handler.validator.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageParsingFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.JsonRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.Validator;

@Component
public class UpdateNetworkValidator extends AbstractNetworkValidator implements JsonRequestValidator<NetworkEntity> {

    private ObjectMapper objectMapper;
    private NetworkService networkService;

    @Autowired
    public UpdateNetworkValidator(OrganisationService organisationService, ObjectMapper objectMapper, Validator beanValidator,
                                  NetworkService networkService) {
        super(organisationService, beanValidator);
        this.objectMapper = objectMapper;
        this.networkService = networkService;

    }

    @Override
    public NetworkEntity validateJson(JsonNode request, String organisationId) throws ValidationException {
        if(request == null) {
            throw new MessageParsingFailedException("Could not parse update network request -message." );
        }

        if(!request.hasNonNull("network")) {
            throw new InvalidMessageBodyException("Update network message is missing network object.");
        }

        JsonNode networkJson = request.get("network");
        NetworkWriteDTO parsedNetwork = objectMapper.convertValue(networkJson, NetworkWriteDTO.class);

        if(parsedNetwork == null) {
            throw new InvalidMessageBodyException("Update network failed. Unable to parse network network-object.");
        }

        NetworkEntity original = networkService.findById(parsedNetwork.getId()).orElseThrow(() ->
            new EntityNotFoundException("Unable to update network. Network with id " + parsedNetwork.getId() + " does not exists"));

        if(parsedNetwork.isPublished()) {
            super.validateObject(parsedNetwork, organisationId);

            if (networkJson.has("organisations")) {
                super.verifyOrganisations(parsedNetwork);
            }
        }

        return original;
    }
}
