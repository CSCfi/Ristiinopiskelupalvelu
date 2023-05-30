package fi.uta.ristiinopiskelu.handler.validator.studyelement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.StudyElement;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import fi.uta.ristiinopiskelu.handler.validator.AbstractObjectValidator;
import org.springframework.util.CollectionUtils;

import javax.validation.Validator;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractStudyElementValidator<T extends AbstractStudyElementWriteDTO> extends AbstractObjectValidator<T> {

    private List<AbstractStudyElementService> studyElementServices;
    private NetworkService networkService;
    
    public AbstractStudyElementValidator(List<AbstractStudyElementService> studyElementServices, NetworkService networkService,
                                         Validator beanValidator) {
        super(beanValidator);
        this.studyElementServices = studyElementServices;
        this.networkService = networkService;
    }

    protected AbstractStudyElementService getServiceForClass(AbstractStudyElementWriteDTO studyElement) {
        return studyElementServices.stream().filter(service -> service.getWriteDtoClass().isInstance(studyElement)).findAny()
                .orElseThrow(() -> new IllegalStateException("No service found for current class " + studyElement.getClass().getSimpleName()));
    }

    protected AbstractStudyElementService getServiceForType(StudyElementType type) {
        return studyElementServices.stream().filter(service -> service.getStudyElementType() == type).findAny()
                .orElseThrow(() -> new IllegalStateException("No service found for type " + type));
    }

    private String getElementInstanceName(AbstractStudyElementWriteDTO studyElement) {
        return studyElement.getClass().getSimpleName();
    }

    protected void validateNotDuplicate(HashSet<KeyHelper> duplicateTest, AbstractStudyElementWriteDTO studyElement, String organisationId) throws ValidationException {
        if (!duplicateTest.add(new KeyHelper(studyElement.getStudyElementId(), organisationId))) {
            throw new DuplicateEntityValidationException(getElementInstanceName(studyElement) + " with [studyElementId: " + studyElement.getStudyElementId()
                + ", organizer " + organisationId + "] is found in message multiple times.");
        }
    }

    public void validateOrganisationReferences(String studyElementId, StudyElementType studyElementType,
                                               List<OrganisationReference> organisationReferences, String organisationId) throws ValidationException {
        if(CollectionUtils.isEmpty(organisationReferences)) {
            throw new OrganizingOrganisationMissingValidationException("studyElementType " + studyElementType + " with [studyElementId: " + studyElementId + "]" +
                    " is missing organizing organisation");
        }

        List<OrganisationReference> organizerOrganisationReferences = organisationReferences.stream()
                .filter(ref -> ref.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(organizerOrganisationReferences)) {
            throw new OrganizingOrganisationMissingValidationException("studyElementType " + studyElementType + " with [studyElementId: " + studyElementId +
                    "] is missing organizing organisation");
        }

        if(organizerOrganisationReferences.size() > 1) {
            throw new MultipleOrganizingOrganisationValidationException("studyElementType " + studyElementType + " with [studyElementId: " + studyElementId + "]"
                    + " has more than one organisation with main organizer role [" + OrganisationRole.ROLE_MAIN_ORGANIZER.getCode() + "]");
        }

        OrganisationReference organizingOrganisation = organizerOrganisationReferences.stream().findFirst().get();

        if (!organizingOrganisation.getOrganisation().getOrganisationTkCode().equals(organisationId)) {
            throw new OrganizingOrganisationMismatchValidationException("studyElementType " + studyElementType + " with [studyElementId: " + studyElementId + "]" +
                    " organizer does not match JMS header organisation id.");
        }
    }

    protected void validateGivenNetworks(AbstractStudyElementWriteDTO studyElement, String organisationId, StudyElementType studyElementType,
                                         boolean networksRequired) throws ValidationException {
        if(networksRequired && (CollectionUtils.isEmpty(studyElement.getCooperationNetworks()) || studyElement.getCooperationNetworks() == null )) {
            throw new CooperationNetworksMissingValidationException("Study element type " + studyElementType + " with [studyElementId: " + studyElement.getStudyElementId()
                    + "] is missing cooperation networks.");
        } else if (!networksRequired && (CollectionUtils.isEmpty(studyElement.getCooperationNetworks()) || studyElement.getCooperationNetworks() == null )){
            return;
        }

        for(CooperationNetwork cooperationNetwork : studyElement.getCooperationNetworks())  {
            NetworkEntity networkEntity = networkService.findValidNetworkById(cooperationNetwork.getId()).orElseThrow(() ->
                new UnknownCooperationNetworkValidationException("Study element type " + studyElementType + " with [studyElementId: " + studyElement.getStudyElementId()
                        + "] contains unknown cooperation network (" + cooperationNetwork.getId() + ")"));

            Predicate<NetworkOrganisation> isOrganisationInNetwork = networkOrg ->
                    networkOrg.getOrganisationTkCode().equals(organisationId)
                            && networkOrg.getValidityInNetwork().getStart().isBefore(OffsetDateTime.now())
                            && ((networkOrg.getValidityInNetwork().getContinuity() == Validity.ContinuityEnum.INDEFINITELY)
                            || (networkOrg.getValidityInNetwork().getEnd() != null && networkOrg.getValidityInNetwork().getEnd().isAfter(OffsetDateTime.now())));

            if(networkEntity.getOrganisations().stream().noneMatch(isOrganisationInNetwork)) {
                throw new NotMemberOfCooperationNetworkValidationException("Sending organisation does not belong to given cooperation network (" + cooperationNetwork.getId() + ")."
                        + " With " + studyElementType.toString() + " [studyElementId: " + studyElement.getStudyElementId() + "]");
            }
        }
    }

    protected void validateParentReferences(List<StudyElementReference> parents) throws ValidationException {
        if(CollectionUtils.isEmpty(parents)) {
            return;
        }

        for(StudyElementReference studyElementReference : parents) {
            Optional<StudyElement> studyElement = getServiceForType(studyElementReference.getReferenceType())
                    .findByStudyElementIdAndOrganizingOrganisationId(
                            studyElementReference.getReferenceIdentifier(),
                            studyElementReference.getReferenceOrganizer());


            if(!studyElement.isPresent()) {
                throw new StudyElementEntityNotFoundException("StudyElement parent reference does not exist " +
                        "[studyElementId: " + studyElementReference.getReferenceIdentifier() +
                        ", organizer: " + studyElementReference.getReferenceOrganizer() +
                        " type: " + studyElementReference.getReferenceType());
            }
        }
    }

    protected String getStudyElementString(AbstractStudyElementWriteDTO studyElement) {
        return "[studyElementId: " + studyElement.getStudyElementId() + "]";
    }
}
