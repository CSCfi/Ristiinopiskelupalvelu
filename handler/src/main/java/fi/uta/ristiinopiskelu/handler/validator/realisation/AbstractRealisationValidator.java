package fi.uta.ristiinopiskelu.handler.validator.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.validator.AbstractObjectValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractRealisationValidator extends AbstractObjectValidator<RealisationWriteDTO> {

    protected RealisationService realisationService;
    protected CourseUnitService courseUnitService;
    protected NetworkService networkService;
    protected ModelMapper modelMapper;

    protected AbstractRealisationValidator() {}

    public AbstractRealisationValidator(ModelMapper modelMapper, RealisationService realisationService,
                                        CourseUnitService courseUnitService, NetworkService networkService, Validator beanValidator) {
        super(beanValidator);
        this.modelMapper = modelMapper;
        this.courseUnitService = courseUnitService;
        this.realisationService = realisationService;
        this.networkService = networkService;
    }

    protected void validateStudyElementReferences(RealisationWriteDTO realisation, String organisationId, MessageType messageType) throws ValidationException {
        if (CollectionUtils.isEmpty(realisation.getStudyElementReferences())) {
            throw new StudyElementReferencesMissingValidationException("Realisation " + getRealisationIdString(realisation) + " is missing all study element references.");
        }

        List<String> notFoundStudyElementRef = new ArrayList<>();
        List<String> refOrgNotMatchingHeaderOrg = new ArrayList<>();
        List<String> noMatchingCooperationNetworks = new ArrayList<>();

        for(StudyElementReference studyElementReference : realisation.getStudyElementReferences()) {
            if(!organisationId.equals(studyElementReference.getReferenceOrganizer())) {
                refOrgNotMatchingHeaderOrg.add(getStudyElementReferenceString(studyElementReference));
                continue;
            }

            // Verifying create course unit message realisation, there is no need to verify reference
            CourseUnitWriteDTO referencedCourseUnit = getCourseUnitByReference(studyElementReference);
            if(referencedCourseUnit == null) {
                notFoundStudyElementRef.add(getStudyElementReferenceString(studyElementReference));
                continue;
            }

            if(!hasAtLeastOneMatchingCooperationNetwork(referencedCourseUnit, realisation)) {
                noMatchingCooperationNetworks.add(getStudyElementReferenceString(studyElementReference));
            }

        }

        if(!CollectionUtils.isEmpty(refOrgNotMatchingHeaderOrg)) {
            throw new OrganizingOrganisationMismatchValidationException("Unable to handle " +  messageType  + ". Given realisation " + getRealisationIdString(realisation)
                    + " has miss match between JMS header Organisation Id and studyElementReference referenceOrganizer. Miss match in study element references: "
                    + String.join(" \n", refOrgNotMatchingHeaderOrg));
        }

        if (!CollectionUtils.isEmpty(notFoundStudyElementRef)) {
            throw new ReferencedStudyElementMissingValidationException("Unable to handle " +  messageType  + ". Given realisation " + getRealisationIdString(realisation)
                    + " has reference(s) to course unit that does not exist. Missing course units: "
                    + String.join(" \n", notFoundStudyElementRef));
        }

        if(!CollectionUtils.isEmpty(noMatchingCooperationNetworks)) {
            throw new CooperationNetworksMismatchValidationException("Unable to handle " +  messageType  + ". Given realisation " + getRealisationIdString(realisation)
                    + " and referenced course unit has no matching cooperation networks. Not matching course units: "
                    + String.join(" \n", noMatchingCooperationNetworks));
        }
    }

    protected boolean hasAtLeastOneMatchingCooperationNetwork(CourseUnitWriteDTO courseUnit, RealisationWriteDTO realisation) {
        if(CollectionUtils.isEmpty(courseUnit.getCooperationNetworks())
                || CollectionUtils.isEmpty(realisation.getCooperationNetworks())) {
            return false;
        }

        return courseUnit.getCooperationNetworks().stream()
                .anyMatch(cuNetwork -> realisation.getCooperationNetworks().stream()
                        .anyMatch(realisationNetwork -> realisationNetwork.getId().equals(cuNetwork.getId())));
    }

    protected CourseUnitWriteDTO getCourseUnitByReference(StudyElementReference studyElementReference) {
        Optional<CourseUnitEntity> courseUnitEntity;
        if(studyElementReference.getReferenceType() == StudyElementType.ASSESSMENT_ITEM) {
            courseUnitEntity = courseUnitService.findByIdAndAssessmentItemIdAndOrganizer(
                    studyElementReference.getReferenceIdentifier(),
                    studyElementReference.getReferenceAssessmentItemId(), studyElementReference.getReferenceOrganizer());
        } else {
            courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                    studyElementReference.getReferenceIdentifier(),
                    studyElementReference.getReferenceOrganizer());
        }

        return courseUnitEntity.map(cu -> modelMapper.map(cu, CourseUnitWriteDTO.class)).orElse(null);

    }

    protected void validateOrganisation(RealisationWriteDTO realisation, String organisationId, MessageType messageType) throws ValidationException {
        if(CollectionUtils.isEmpty(realisation.getOrganisationReferences())) {
            throw new OrganizingOrganisationMissingValidationException("Unable to handle: " + messageType +
                    ". Realisation with " + getRealisationIdString(realisation) + " is missing organizing organisation");
        }

        List<OrganisationReference> organizerOrganisationReferences = realisation.getOrganisationReferences().stream()
                .filter(ref -> ref.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(organizerOrganisationReferences)) {
            throw new OrganizingOrganisationMissingValidationException("Unable to handle: " + messageType +
                    ". Realisation with " + getRealisationIdString(realisation) +
                    " is missing organizing organisation");
        }

        if(organizerOrganisationReferences.size() > 1) {
            throw new MultipleOrganizingOrganisationValidationException("Unable to handle: " + messageType +
                    ". Realisation with " + getRealisationIdString(realisation) +
                    " has more than one organisation with main organizer role [" + OrganisationRole.ROLE_MAIN_ORGANIZER.getCode() + "]");
        }

        OrganisationReference organizingOrganisation = organizerOrganisationReferences.stream().findFirst().get();

        if(!organizingOrganisation.getOrganisation().getOrganisationTkCode().equals(organisationId)) {
            throw new OrganizingOrganisationMismatchValidationException("Unable to handle: " + messageType +
                    ". Realisation with " + getRealisationIdString(realisation) +
                    " organizer does not match JMS header organisation id.");
        }
    }

    protected void validateGivenNetworks(RealisationWriteDTO realisation, String organisationId, MessageType messageType, boolean networksRequired) throws ValidationException {
        if(networksRequired && CollectionUtils.isEmpty(realisation.getCooperationNetworks())) {
            throw new CooperationNetworksMissingValidationException("Unable to handle: " + messageType +
                    ". Realisation with " + getRealisationIdString(realisation) + " is missing cooperation networks.");
        }

        for(CooperationNetwork cooperationNetwork : realisation.getCooperationNetworks())  {
            NetworkEntity networkEntity = networkService.findNetworkById(cooperationNetwork.getId()).orElse(null);
            if(networkEntity == null) {
                throw new UnknownCooperationNetworkValidationException("Unable to handle: " + messageType + "." +
                        " Realisation with " + getRealisationIdString(realisation) +
                        " contains unknown cooperation network (" + cooperationNetwork.getId() + ")");
            }

            Predicate<NetworkOrganisation> isOrganisationInNetwork = networkOrg ->
                    networkOrg.getOrganisationTkCode().equals(organisationId);

            if(networkEntity.getOrganisations().stream().noneMatch(isOrganisationInNetwork)) {
                throw new NotMemberOfCooperationNetworkValidationException("Unable to handle: " + messageType +
                        ". Realisation with " + getRealisationIdString(realisation) +
                        " Sending organisation does not belong to given cooperation network (" + cooperationNetwork.getId() + ").");
            }
        }
    }

    protected String getStudyElementReferenceString(StudyElementReference studyElementReference) {
        return "[ReferenceIdentifier: " + studyElementReference.getReferenceIdentifier()
                + " referenceOrganizer: " + studyElementReference.getReferenceOrganizer() + "]";
    }

    protected String getRealisationIdString(RealisationWriteDTO realisation) {
        return "[id: " + realisation.getRealisationId() + "]";
    }
}
