package fi.uta.ristiinopiskelu.handler.validator.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class CreateRealisationValidator extends AbstractRealisationValidator implements RequestValidator<CreateRealisationRequest> {

    @Autowired
    public CreateRealisationValidator(ModelMapper modelMapper,
                                      RealisationService realisationService,
                                      CourseUnitService courseUnitService,
                                      NetworkService networkService, Validator beanValidator) {
        super(modelMapper, realisationService, courseUnitService, networkService, beanValidator);
    }

    @Override
    public void validateRequest(CreateRealisationRequest request, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }

        List<RealisationWriteDTO> realisations = request.getRealisations();
        if(CollectionUtils.isEmpty(realisations)) {
            throw new InvalidMessageBodyException("Received create realisation request -message without any realisations.");
        }

        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        for(RealisationWriteDTO realisation : realisations) {
            super.validateObject(realisation, organisationId);
            validateOrganisation(realisation, organisationId, MessageType.CREATE_REALISATION_REQUEST);
            validateNotDuplicate(duplicateTest, realisation, organisationId);
            validateGivenNetworks(realisation, organisationId, MessageType.CREATE_REALISATION_REQUEST);
            validateStudyElementReferences(realisation, organisationId, MessageType.CREATE_REALISATION_REQUEST);
        }
    }

    public List<RealisationWriteDTO> validateCreateCourseUnitRealisations(List<RealisationWriteDTO> realisations, String organisationId, CourseUnitWriteDTO courseUnit) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }

        for(RealisationWriteDTO realisation : realisations) {
            super.validateObject(realisation, organisationId);
            validateOrganisation(realisation, organisationId, MessageType.CREATE_COURSEUNIT_REQUEST);
            validateGivenNetworks(realisation, organisationId, MessageType.CREATE_COURSEUNIT_REQUEST);
            validateHasAtleastOneMatchingCooperationNetwork(courseUnit, realisation, organisationId);
        }

        return realisations;
    }

    protected void validateNotDuplicate(HashSet<KeyHelper> duplicateTest, RealisationWriteDTO realisation, String organisationId) throws ValidationException {
        if(!duplicateTest.add(new KeyHelper(realisation.getRealisationId(), organisationId))) {
            throw new DuplicateEntityValidationException("Realisation with " + getRealisationIdString(realisation) + " is found in message multiple times.");
        }

        Optional<RealisationEntity> realisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
                realisation.getRealisationId(), organisationId);

        if(realisationEntity.isPresent()) {
            throw new EntityAlreadyExistsValidationException("Cannot create realisation with " + getRealisationIdString(realisation) +
                    " realisation already exists for organisation " + organisationId);
        }
    }

    protected void validateGivenNetworks(RealisationWriteDTO parsedRealisation, String organisationId, MessageType messageType) throws ValidationException {
        super.validateGivenNetworks(parsedRealisation, organisationId, messageType, true);
    }

    protected void validateStudyElementReferences(RealisationWriteDTO parsedRealisation, String organisationId, MessageType messageType) throws ValidationException {
        super.validateStudyElementReferences(parsedRealisation, organisationId, messageType);
    }

    protected void validateOrganisation(RealisationWriteDTO realisation, String organisationId, MessageType messageType) throws ValidationException {
        super.validateOrganisation(realisation, organisationId, MessageType.CREATE_COURSEUNIT_REQUEST);
    }

    protected void validateHasAtleastOneMatchingCooperationNetwork(CourseUnitWriteDTO courseUnit, RealisationWriteDTO realisation, String organisationId) throws ValidationException {
        if(!super.hasAtLeastOneMatchingCooperationNetwork(courseUnit, realisation)) {
            throw new CooperationNetworksMismatchValidationException("Unable to handle " + MessageType.CREATE_COURSEUNIT_REQUEST.name()
                    + ". Given sub realisation " + getRealisationIdString(realisation)
                    + " and given course unit [" + courseUnit.getStudyElementId() + "]" +
                    " has no matching cooperation networks.");
        }
    }
}
