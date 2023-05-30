package fi.uta.ristiinopiskelu.handler.validator.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditTargetType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.*;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.CreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;

@Component
public class CreateStudyRecordValidator implements RequestValidator<CreateStudyRecordRequest> {

    private OrganisationService organisationService;
    private CourseUnitService courseUnitService;
    private RealisationService realisationService;
    private StudyModuleService studyModuleService;
    private DegreeService degreeService;
    private RegistrationService registrationService;
    private NetworkService networkService;

    @Autowired
    public CreateStudyRecordValidator(OrganisationService organisationService, CourseUnitService courseUnitService, RealisationService realisationService,
                                      StudyModuleService studyModuleService, DegreeService degreeService, RegistrationService registrationService,
                                      NetworkService networkService) {
        this.organisationService = organisationService;
        this.courseUnitService = courseUnitService;
        this.realisationService = realisationService;
        this.studyModuleService = studyModuleService;
        this.degreeService = degreeService;
        this.registrationService = registrationService;
        this.networkService = networkService;
    }

    @Override
    public void validateRequest(CreateStudyRecordRequest studyRecord, String organisationId) throws ValidationException {
        if(StringUtils.isEmpty(organisationId)) {
            throw new MissingMessageHeaderException("Cannot perform create. Organisation Id is missing from header. This should not happen.");
        }

        if(!studyRecord.getSendingOrganisation().equals(organisationId)) {
            throw new OrganizingOrganisationMismatchValidationException("Unable to process study record request. sendingOrganisation not match given JMS header organisation id.");
        }

        OrganisationEntity sendingOrganisation = organisationService.findById(studyRecord.getSendingOrganisation())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, studyRecord.getSendingOrganisation()));

        NetworkEntity givenNetwork = null;

        if(StringUtils.hasText(studyRecord.getNetworkIdentifier())) {
            givenNetwork = networkService.findValidNetworkById(studyRecord.getNetworkIdentifier())
                .orElseThrow(() -> new ValidNetworkNotFoundValidationException("Could not find valid network with given network id: " + studyRecord.getNetworkIdentifier()));

            if(!isOrganisationValidInNetwork(givenNetwork, studyRecord.getSendingOrganisation())) {
                throw new OrganisationNotValidInNetworkValidationException(
                    "Unable to process study record request. sendingOrganisation does not belong to given network [" + studyRecord.getNetworkIdentifier() + "]. " +
                        "Organisation " + studyRecord.getSendingOrganisation() + " not found or it's no longer valid in network.");
            }

            if(!isOrganisationValidInNetwork(givenNetwork, studyRecord.getReceivingOrganisation())) {
                throw new OrganisationNotValidInNetworkValidationException(
                    "Unable to process study record request. receivingOrganisation does not belong to given network [" + studyRecord.getNetworkIdentifier() + "]. " +
                        "Organisation " + studyRecord.getReceivingOrganisation() + " not found or it's no longer valid in network.");
            }
        }

        for(CompletedCredit completedCredit : studyRecord.getCompletedCredits()) {
            RegistrationEntity validRegistration = getValidRegistrations(studyRecord.getStudent(), completedCredit, sendingOrganisation.getId());

            if(validRegistration == null) {
                throw new RegistrationStatusValidationException("No valid registrations found for completedCreditTarget [id=" +
                    completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId() + ", organizingOrganisationId=" +
                    sendingOrganisation.getId() + ", type=" + completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType() + "]");
            }

            if(givenNetwork != null) {
                final String givenNetworkId = givenNetwork.getId();
                if(StringUtils.hasText(validRegistration.getNetworkIdentifier()) && !validRegistration.getNetworkIdentifier().equals(givenNetworkId)) {
                    throw new RegistrationStatusValidationException("No valid registrations found for completedCreditTarget [id=" +
                        completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId() + ", organizingOrganisationId=" +
                        sendingOrganisation.getId() + ", type=" + completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType() + "] within network " + givenNetworkId);                }
            } else {
                // if no networks were given, assume the network first in list
                studyRecord.setNetworkIdentifier(validRegistration.getNetworkIdentifier());
            }
        }
    }

    private RegistrationEntity getValidRegistrations(StudyRecordStudent student, CompletedCredit completedCredit,
                                                               String sendingOrganisation) {

        String completedCreditTargetId = completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId();
        CompletedCreditTargetType completedCreditTargetType = completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType();
        CompositeIdentifiedEntity entity = null;

        switch (completedCreditTargetType) {
            case DEGREE:
                entity = degreeService.findByStudyElementIdAndOrganizingOrganisationId(completedCreditTargetId, sendingOrganisation)
                    .orElseThrow(() -> new StudyElementEntityNotFoundException(DegreeEntity.class, completedCreditTargetId, sendingOrganisation)); break;
            case STUDY_MODULE:
                entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(completedCreditTargetId, sendingOrganisation)
                    .orElseThrow(() -> new StudyElementEntityNotFoundException(StudyModuleEntity.class, completedCreditTargetId, sendingOrganisation)); break;
            case COURSE_UNIT:
                entity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(completedCreditTargetId, sendingOrganisation)
                    .orElseThrow(() -> new StudyElementEntityNotFoundException(CourseUnitEntity.class, completedCreditTargetId, sendingOrganisation)); break;
            case REALISATION:
                entity = realisationService.findByIdAndOrganizingOrganisationId(completedCreditTargetId, sendingOrganisation)
                    .orElseThrow(() -> new EntityNotFoundException(RealisationEntity.class, completedCreditTargetId, sendingOrganisation));
                break;
            // ASSESSMENT_ITEM is unimplementable atm, if ever even needed
            case ASSESSMENT_ITEM:
            default:
                throw new IllegalArgumentException("Invalid completedCreditTargetType given: " + completedCreditTargetType);
        }

        Optional<RegistrationEntity> latestRegistrationOptional = registrationService.findByStudentAndSelectionsReplies(student, entity.getElementId(),
                entity.getOrganizingOrganisationId(), completedCreditTargetType.name()).stream().max(Comparator.comparing(RegistrationEntity::getEnrolmentDateTime));

        if(latestRegistrationOptional.isPresent()) {
            RegistrationEntity latestRegistration = latestRegistrationOptional.get();
            if(latestRegistration.getStatus() == RegistrationStatus.REGISTERED &&
                latestRegistration.getSelectionsReplies().stream().anyMatch(sr -> sr.getSelectionItemId().equals(completedCreditTargetId) &&
                    sr.getSelectionItemStatus() == RegistrationSelectionItemStatus.ACCEPTED)) {
                return latestRegistration;
            }
        }

        return null;
    }
                                                                                    
    protected boolean isOrganisationValidInNetwork(NetworkEntity networkEntity, String organisation) {
        return networkEntity.getOrganisations().stream().anyMatch(o -> o.getOrganisationTkCode().equals(organisation)
            && (o.getValidityInNetwork() != null)
            && (o.getValidityInNetwork().getStart() != null && DateUtils.isBeforeOrEqual(o.getValidityInNetwork().getStart(), OffsetDateTime.now()))
            && (o.getValidityInNetwork().getEnd() == null || DateUtils.isAfterOrEqual(o.getValidityInNetwork().getEnd(), OffsetDateTime.now())));
    }
}
