package fi.uta.ristiinopiskelu.handler.validator.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.Validator;
import java.util.Optional;

@Component
public class CreateNetworkValidator extends AbstractNetworkValidator implements RequestValidator<CreateNetworkRequest> {

    @Autowired
    private NetworkService networkService;

    @Autowired
    public CreateNetworkValidator(OrganisationService organisationService, Validator beanValidator) {
        super(organisationService, beanValidator);
    }

    @Override
    public void validateRequest(CreateNetworkRequest request, String organisationId) throws ValidationException {
        NetworkWriteDTO network = request.getNetwork();
        if(network == null) {
            throw new InvalidMessageBodyException("Received create network request -message without network-object.");
        }

        Optional<NetworkEntity> existing = networkService.findById(network.getId());
        if(existing.isPresent()) {
            throw new ValidationException("Network already exists with id " + network.getId());
        }

        if(network.isPublished()) {
            super.validateObject(network, organisationId);
            super.verifyOrganisations(network);
        }
    }
}
