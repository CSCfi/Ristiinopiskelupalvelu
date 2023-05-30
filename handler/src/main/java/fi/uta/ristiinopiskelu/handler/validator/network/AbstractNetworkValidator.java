package fi.uta.ristiinopiskelu.handler.validator.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.NetworkOrganisationValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.ValidationException;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.AbstractObjectValidator;
import org.springframework.util.CollectionUtils;

import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractNetworkValidator extends AbstractObjectValidator<NetworkWriteDTO> {

    private OrganisationService organisationService;

    public OrganisationService getOrganisationService() {
        return organisationService;
    }

    public AbstractNetworkValidator(OrganisationService organisationService, Validator beanValidator) {
        super(beanValidator);
        this.organisationService = organisationService;
    }

    protected void verifyOrganisations(NetworkWriteDTO network) throws ValidationException {
        if(CollectionUtils.isEmpty(network.getOrganisations())) {
            throw new NetworkOrganisationValidationException("Network must have at least two organisations.");
        }

        List<String> organisationIds = network.getOrganisations().stream().map(NetworkOrganisation::getOrganisationTkCode).collect(Collectors.toList());
        if(organisationIds != null || !organisationIds.isEmpty()) {
            List<OrganisationEntity> organisations = organisationService.findByIds(organisationIds);

            // Check that all organisations in request exists
            if(organisations.isEmpty() || organisations.size() != organisationIds.size()) {
                List<String> missing = organisationIds.stream()
                        .filter(id -> organisations.stream().noneMatch(o -> o.getId().equals(id)))
                        .collect(Collectors.toList());

                throw new ValidationException("Could not find all organisations that should be in network." +
                        "Missing organisations: " + String.join(", ", missing));
            }
        }
    }
}
