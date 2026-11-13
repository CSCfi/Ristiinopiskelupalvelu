package fi.uta.ristiinopiskelu.handler.validator.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditTargetType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.*;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.CreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            throw new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);
        }

        if(!studyRecord.getSendingOrganisation().equals(organisationId)) {
            throw new OrganizingOrganisationMismatchValidationException("Unable to process study record request. sendingOrganisation not match given JMS header organisation id.");
        }

        OrganisationEntity sendingOrganisation = organisationService.findById(studyRecord.getSendingOrganisation())
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, studyRecord.getSendingOrganisation()));

        NetworkEntity givenNetwork = null;

        if(StringUtils.hasText(studyRecord.getNetworkIdentifier())) {
            givenNetwork = networkService.findValidNetworkById(studyRecord.getNetworkIdentifier(), true, false)
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
            List<RegistrationEntity> validRegistrations = getValidRegistrations(studyRecord.getStudent(), completedCredit, sendingOrganisation.getId());

            if(CollectionUtils.isEmpty(validRegistrations)) {
                throw new RegistrationStatusValidationException("No valid registrations found for completedCreditTarget [id=" +
                    completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId() + ", organizingOrganisationId=" +
                    sendingOrganisation.getId() + ", type=" + completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType() + "]");
            }

            if(givenNetwork != null) {
                final String givenNetworkId = givenNetwork.getId();
                if(validRegistrations.stream()
                    .noneMatch(validRegistration -> StringUtils.hasText(validRegistration.getNetworkIdentifier()) &&
                        validRegistration.getNetworkIdentifier().equals(givenNetworkId))) {
                    throw new RegistrationStatusValidationException("No valid registrations found for completedCreditTarget [id=" +
                        completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId() + ", organizingOrganisationId=" +
                        sendingOrganisation.getId() + ", type=" + completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType() + "] within network " + givenNetworkId);
                }
            } else {
                // if no networks were given, assume the network first in list
                studyRecord.setNetworkIdentifier(validRegistrations.getFirst().getNetworkIdentifier());
            }
        }
    }

    private List<RegistrationEntity> getValidRegistrations(StudyRecordStudent student, CompletedCredit completedCredit,
                                                                                String sendingOrganisation) {

        String completedCreditTargetId = completedCredit.getCompletedCreditTarget().getCompletedCreditTargetId();
        CompletedCreditTargetType completedCreditTargetType = completedCredit.getCompletedCreditTarget().getCompletedCreditTargetType();
        CompositeIdentifiedEntity entity;

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
                throw new UnsupportedCompletedCreditTargetTypeValidationException(
                    "Unsupported completedCreditTargetType: " + completedCreditTargetType);
        }

        List<RegistrationEntity> registrations = registrationService.findByStudentAndSelections(student, entity.getElementId(),
                entity.getOrganizingOrganisationId(), completedCreditTargetType.name());

        // group registration selections
        Map<RegistrationSelectionGroupKey, List<RegistrationSelectionReplyMatch>> selectionGroups = registrations.stream()
            .sorted(new MessageReceivingOrder())
            .filter(r -> !CollectionUtils.isEmpty(r.getSelectionsReplies()))
            .flatMap(r -> r.getSelectionsReplies().stream()
                .map(selection -> new RegistrationSelectionReplyMatch(r, selection)))
            .collect(Collectors.groupingBy(rs ->
                new RegistrationSelectionGroupKey(rs.registrationSelectionReply().getSelectionItemId(), rs.registrationSelectionReply().getSelectionItemType()),
                LinkedHashMap::new,
                Collectors.toList()));

        return selectionGroups.values().stream()
            .filter(registrationSelectionMatches -> {
                RegistrationSelectionReplyMatch latest = registrationSelectionMatches.getLast();

                return latest.registrationEntity().getStatus() == RegistrationStatusEntity.REGISTERED &&
                    hasAcceptedSelectionForCompletedCreditTarget(completedCreditTargetId, completedCreditTargetType, latest.registrationEntity().getSelections())
                        .test(latest.registrationSelectionReply());
            })
            .map(registrationSelectionMatches -> registrationSelectionMatches.getLast().registrationEntity())
            .toList();
    }

    private record RegistrationSelectionGroupKey(String selectionItemId, RegistrationSelectionItemTypeEntity selectionItemType) {
    }

    private record RegistrationSelectionReplyMatch(RegistrationEntity registrationEntity, RegistrationReplySelectionEntity registrationSelectionReply) {
    }

    private static class MessageReceivingOrder implements Comparator<RegistrationEntity> {

        @Override
        public int compare(RegistrationEntity registration, RegistrationEntity otherRegistration) {
            if(hasBrokerOrder(registration) && hasBrokerOrder(otherRegistration)) {
                int c = Long.compare(registration.getReceivingTimestamp(), otherRegistration.getReceivingTimestamp());

                if (c != 0) {
                    return c;
                }

                return Long.compare(registration.getReceivingSequence(), otherRegistration.getReceivingSequence());
            }

            if(hasBrokerOrder(registration)) {
                return 1;
            }

            if(hasBrokerOrder(otherRegistration)) {
                return -1;
            }

            // fallback to the old ordering
            return Comparator.comparing(RegistrationEntity::getReceivingDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(RegistrationEntity::getEnrolmentDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(registration, otherRegistration);
        }

        private boolean hasBrokerOrder(RegistrationEntity registration) {
            return registration.getReceivingTimestamp() != null && registration.getReceivingSequence() != null;
        }
    }

    private Predicate<RegistrationReplySelectionEntity> hasAcceptedSelectionForCompletedCreditTarget(String completedCreditTargetId,
                                                                                                     CompletedCreditTargetType completedCreditTargetType,
                                                                                                     List<CreateRegistrationSelectionEntity> requestSelections) {
        return registrationSelectionReply -> {
            if(registrationSelectionReply == null) {
                return false;
            }

            // remember, selectionsReplies _DO_NOT_ have parent info available, hence we need to find the matching one from the original request selections
            CreateRegistrationSelectionParentEntity parent = Optional.ofNullable(findMatchingRequestSelection(registrationSelectionReply.getSelectionItemId(),
                    registrationSelectionReply.getSelectionItemType(), requestSelections))
                .map(CreateRegistrationSelectionEntity::getParent)
                .orElse(null);

            boolean isSelectionInParents = false;

            while(parent != null) {
                if(isValidSelection(parent, completedCreditTargetId, completedCreditTargetType, null)) {
                    isSelectionInParents = true;
                    break;
                }

                parent = parent.getParent();
            }

            return isValidSelection(registrationSelectionReply, completedCreditTargetId, completedCreditTargetType, RegistrationSelectionItemStatusEntity.ACCEPTED) ||
                (registrationSelectionReply.getSelectionItemStatus() == RegistrationSelectionItemStatusEntity.ACCEPTED && isSelectionInParents);
        };
    }

    private CreateRegistrationSelectionEntity findMatchingRequestSelection(String selectionItemId, RegistrationSelectionItemTypeEntity selectionItemType,
                                                                          List<CreateRegistrationSelectionEntity> requestSelections) {
        if(CollectionUtils.isEmpty(requestSelections)) {
            return null;
        }

        return requestSelections.stream()
            .filter(rs -> selectionItemId.equals(rs.getSelectionItemId()) &&
                selectionItemType == rs.getSelectionItemType())
            .findFirst()
            .orElse(null);
    }

    private boolean isValidSelection(RegistrationSelectionEntity selection, String completedCreditTargetId, CompletedCreditTargetType completedCreditTargetType,
                                     @Nullable RegistrationSelectionItemStatusEntity status) {
        if(selection == null) {
            return false;
        }

        CompletedCreditTargetType selectionTypeAsCompletedCreditTargetType = CompletedCreditTargetType.from(selection.getSelectionItemType());

        if(selectionTypeAsCompletedCreditTargetType == null ) {
            return false;
        }

        boolean matchesCompletedCreditTarget = completedCreditTargetId.equals(selection.getSelectionItemId()) &&
            completedCreditTargetType == selectionTypeAsCompletedCreditTargetType;

        if(!matchesCompletedCreditTarget) {
            return false;
        }

        if(status == null) {
            return true;
        }

        return selection instanceof StatusAwareRegistrationSelectionEntity statusAwareSelection &&
            statusAwareSelection.getSelectionItemStatus() == status;
    }
                                                                                    
    protected boolean isOrganisationValidInNetwork(NetworkEntity networkEntity, String organisation) {
        return networkEntity.getOrganisations().stream().anyMatch(o -> o.getOrganisationTkCode().equals(organisation)
            && (o.getValidityInNetwork() != null)
            && (o.getValidityInNetwork().getStart() != null && DateUtils.isBeforeOrEqual(o.getValidityInNetwork().getStart(), OffsetDateTime.now())));
    }
}
