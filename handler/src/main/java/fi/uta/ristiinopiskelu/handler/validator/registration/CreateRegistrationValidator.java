package fi.uta.ristiinopiskelu.handler.validator.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CompletionOptionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.SelectionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.SelectionValueEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.SelectionTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementTypeEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.validator.RequestValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateRegistrationValidator extends AbstractRegistrationValidator implements RequestValidator<CreateRegistrationRequest> {

    private CourseUnitService courseUnitService;
    private RealisationService realisationService;
    private NetworkService networkService;

    @Autowired
    public CreateRegistrationValidator(CourseUnitService courseUnitService, RealisationService realisationService, NetworkService networkService) {
        this.courseUnitService = courseUnitService;
        this.realisationService = realisationService;
        this.networkService = networkService;
    }

    @Override
    public void validateRequest(CreateRegistrationRequest registration, String organisationId) throws ValidationException {
        if(!StringUtils.hasText(organisationId)) {
            throw new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);
        }

        NetworkEntity networkEntity = networkService.findValidNetworkById(registration.getNetworkIdentifier())
                .orElseThrow(() -> new ValidNetworkNotFoundValidationException("Could not find valid network with given network id: " + registration.getNetworkIdentifier()));

        if(!registration.getSendingOrganisationTkCode().equals(organisationId)) {
            throw new OrganizingOrganisationMismatchValidationException(
                "Unable to process registration request. SendingOrganisation does not match the JMS header organisation id.");
        }

        if(!isOrganisationValidInNetwork(networkEntity, registration.getSendingOrganisationTkCode())) {
            throw new OrganisationNotValidInNetworkValidationException(
                    "Unable to process registration request. SendingOrganisation does not belong to given network [" + registration.getNetworkIdentifier() + "]. " +
                    "Organisation " + registration.getSendingOrganisationTkCode() + " not found or it's no longer valid in network.");
        }

        if(!isOrganisationValidInNetwork(networkEntity, registration.getReceivingOrganisationTkCode())) {
            throw new OrganisationNotValidInNetworkValidationException(
                    "Unable to process registration request. ReceivingOrganisation does not belong to given network [" + registration.getNetworkIdentifier() + "]. " +
                    "Organisation " + registration.getReceivingOrganisationTkCode() + " not found or it's no longer valid in network.");
        }

        if(CollectionUtils.isEmpty(registration.getSelections())) {
            throw new InvalidMessageBodyException("Unable to process registration request. Request has no selections.");
        }

        verifySelectionsHierarchy(registration.getSelections(), registration.getReceivingOrganisationTkCode());
        verifyAllSelectionsBelongToGivenNetwork(networkEntity.getId(), registration, registration.getReceivingOrganisationTkCode());
        verifySelectionsEnrollable(registration.getSelections(), registration.getEnrolmentDateTime(), registration.getReceivingOrganisationTkCode());
    }

    protected void verifySelectionsEnrollable(List<CreateRegistrationSelection> selections, OffsetDateTime enrollmentDateTime, String receivingOrganisationTkCode) {
        if(enrollmentDateTime == null) {
            throw new RegistrationSelectionEnrollmentTimeValidationException("Unable to process registration request." +
                    " Could not verify enrollment time validity because enrollmentDateTime was not given with request.");
        }

        List<CreateRegistrationSelection> realisationSelections = selections.stream()
                .filter(selection -> selection.getSelectionItemType() == RegistrationSelectionItemType.REALISATION)
                .toList();

        List<CreateRegistrationSelection> notEnrollableSelections = new ArrayList<>();

        for (CreateRegistrationSelection selection : realisationSelections) {
            RealisationEntity realisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
                    selection.getSelectionItemId(), receivingOrganisationTkCode).orElse(null);

            if(realisationEntity == null) {
                // Should always exist because it should have been validated before
                throw new RuntimeException("Missing realisationEntity. This should not happen.");
            }

            // always allow sending student aborts regardless of realisation enrollment dates. otherwise
            // enrollmentDateTime must be within realisation enrollment start/end.
            if(selection.getSelectionItemStatus() == RegistrationSelectionItemStatus.ABORTED_BY_STUDENT ||
                ((realisationEntity.getEnrollmentStartDateTime() != null
                    && DateUtils.isAfterOrEqual(enrollmentDateTime, realisationEntity.getEnrollmentStartDateTime()))
                && (realisationEntity.getEnrollmentEndDateTime() == null
                    || DateUtils.isBeforeOrEqual(enrollmentDateTime, realisationEntity.getEnrollmentEndDateTime())))) {
                continue;
            }

            notEnrollableSelections.add(selection);
        }

        if(!notEnrollableSelections.isEmpty()) {
            throw new RegistrationSelectionEnrollmentTimeValidationException("Unable to process registration request." +
                    " Registration contains selections for realisation(s) that are not enrollable in given enrollment time." +
                    " Invalid selections: " + String.join(", ", notEnrollableSelections.stream().map(s -> getSelectionIdString(s)).collect(Collectors.toList())));
        }
    }

    protected void verifyAllSelectionsBelongToGivenNetwork(String registrationNetworkId, CreateRegistrationRequest registration, String organisationId) {
        List<String> selectionsNotValidInGivenNetwork = new ArrayList<>();

        for (CreateRegistrationSelection selection : registration.getSelections()) {
            CooperationNetworkEntity cooperationNetwork = null;
            if(selection.getSelectionItemType() == RegistrationSelectionItemType.REALISATION) {
                RealisationEntity realisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
                        selection.getSelectionItemId(), organisationId).orElse(null);

                if(realisationEntity == null) {
                    // Should always exist because it should have been validated before
                    throw new RuntimeException("Missing realisationEntity. This should not happen.");
                }

                cooperationNetwork = findCooperationNetworkFromArrayById(realisationEntity.getCooperationNetworks(), registrationNetworkId);
            } else {
                CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                        selection.getSelectionItemId(), organisationId).orElse(null);

                if(courseUnitEntity == null) {
                    // Should always exist because it should have been validated before
                    throw new RuntimeException("Missing realisationEntity. This should not happen.");
                }

                cooperationNetwork = findCooperationNetworkFromArrayById(courseUnitEntity.getCooperationNetworks(), registrationNetworkId);
            }

            if(cooperationNetwork == null) {
                selectionsNotValidInGivenNetwork.add("[SelectionItemId: " + selection.getSelectionItemId() +
                        " organisation: " + organisationId + "]");
                continue;
            }

            LocalDate validityStartDate = cooperationNetwork.getValidityStartDate();
            LocalDate validityEndDate = cooperationNetwork.getValidityEndDate();

            if(BooleanUtils.isTrue(cooperationNetwork.getEnrollable())
                && ((validityStartDate == null && validityEndDate == null)
                    || (validityStartDate != null && DateUtils.isBeforeOrEqual(validityStartDate, registration.getEnrolmentDateTime().toLocalDate())
                        && (validityEndDate == null ||  DateUtils.isAfterOrEqual(validityEndDate, registration.getEnrolmentDateTime().toLocalDate()))))
            ){
                continue;
            } else {
                selectionsNotValidInGivenNetwork.add("[SelectionItemId: " + selection.getSelectionItemId() +
                        " organisation: " + organisationId + "]");
            }
        }

        if(!CollectionUtils.isEmpty(selectionsNotValidInGivenNetwork)) {
            throw new RegistrationSelectionNotValidInNetworkValidationException(
                    "Unable to process registration request." +
                    " Request contains selection(s) that do not belong to or are not enrollable" +
                    " for given network (" + registrationNetworkId + "). " + String.join(", ", selectionsNotValidInGivenNetwork));
        }
    }

    private CooperationNetworkEntity findCooperationNetworkFromArrayById(List<CooperationNetworkEntity> cooperationNetworks, String networkIdToFind) {
        if(CollectionUtils.isEmpty(cooperationNetworks)) {
            return null;
        }

        return cooperationNetworks.stream()
                .filter(cn -> cn.getId().equals(networkIdToFind))
                .findFirst()
                .orElse(null);
    }

    protected void verifySelectionsHierarchy(List<CreateRegistrationSelection> selections, String receivingOrganisation) throws ValidationException {
        for(CreateRegistrationSelection selection : selections) {
            ParentAwareRegistrationSelection courseUnitSelection = findCourseUnitSelection(selection);
            if(courseUnitSelection == null) {
                throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                        " Registration contains selection hierarchy that is missing course unit.");
            }

            // Verify registration selection course unit exist in database
            CourseUnitEntity selectedCourseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
                    courseUnitSelection.getSelectionItemId(), receivingOrganisation).orElse(null);

            if(selectedCourseUnitEntity == null) {
                throw new RegistrationSelectionDoesNotExistValidationException("Unable to process registration request." +
                        " Registration contains course unit selection " + getSelectionIdString(courseUnitSelection) +
                        " that does not exists for organisation " + receivingOrganisation);
            }

            if(courseUnitSelection.getParent() != null) {
                throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                        " Registration contains course unit that has parents in hierarchy.");
            }

            if(selection.getSelectionItemType() == RegistrationSelectionItemType.COURSE_UNIT) {
                // No need to verify any more references since registration selection is course unit and it was already verified
                continue;
            } else if(selection.getSelectionItemType() == RegistrationSelectionItemType.REALISATION) {

                // Verify selection realisation exist in database
                RealisationEntity selectedRealisationEntity = realisationService.findByIdAndOrganizingOrganisationId(
                        selection.getSelectionItemId(), receivingOrganisation)
                        .orElseThrow(() -> new RegistrationSelectionDoesNotExistValidationException("Unable to process registration request." +
                            " Registration contains realisation selection" + getSelectionIdString(selection) +
                            " that does not exists for organisation " + receivingOrganisation));

                if(selection.getParent().getSelectionItemType() == RegistrationSelectionItemType.COURSE_UNIT) {
                    // If parent is course unit, verify that current realisation has reference to given parent course unit (course unit has already been verified)
                    if(CollectionUtils.isEmpty(selectedRealisationEntity.getStudyElementReferences()) ||
                        selectedRealisationEntity.getStudyElementReferences().stream().noneMatch(ref ->
                            ref.getReferenceIdentifier().equals(selectedCourseUnitEntity.getStudyElementId())
                            && ref.getReferenceOrganizer().equals(selectedCourseUnitEntity.getOrganizingOrganisationId())
                            && ref.getReferenceType() == StudyElementTypeEntity.COURSE_UNIT)) {

                        throw new RegistrationSelectionReferenceValidationException("Unable to process registration request." +
                                " Registration has selected realisation that does not have reference to referenced course unit." +
                                " Failed realisation selection: " + getSelectionIdString(selection));
                    }
                } else {
                    // If parent is assessment item, verify all references are correct in hierarchy
                    verifyHierarchyReferences(selection, selectedCourseUnitEntity, selectedRealisationEntity);
                }

                List<String> registrationSubGroupSelections = selection.getSubGroupSelections() != null ? selection.getSubGroupSelections() : Collections.emptyList();
                List<SelectionEntity> selectedRealisationEntityGroupSelections =
                    selectedRealisationEntity.getGroupSelections() != null ? selectedRealisationEntity.getGroupSelections() : Collections.emptyList();

                // make sure that all of the selected sub groups actually exist
                for(String regSubGroupSelection : registrationSubGroupSelections) {
                    boolean subGroupExists = selectedRealisationEntityGroupSelections.stream()
                            .anyMatch(sel -> sel.getSelectionValues().stream().anyMatch(sv -> regSubGroupSelection.equals(sv.getId())));
                    if(!subGroupExists) {
                        throw new RegistrationSelectionSubGroupDoesNotExistException("Unable to process registration request." +
                                " Subgroup selection does not exist: " + regSubGroupSelection + "." +
                                " Failed realisation selection: " + getSelectionIdString(selection));
                    }
                }

                // finally check if sub group selection amounts match the selection type rules (not required if aborting)
                if (selection.getSelectionItemStatus() != RegistrationSelectionItemStatus.ABORTED_BY_STUDENT) {
                    for (SelectionEntity sel : selectedRealisationEntityGroupSelections) {
                        String[] selectionsInThisSelection = sel.getSelectionValues().stream()
                            .filter(sv -> registrationSubGroupSelections.stream().anyMatch(sgs -> sgs.equals(sv.getId())))
                            .map(SelectionValueEntity::getId)
                            .toArray(String[]::new);

                        if (sel.getType() == SelectionTypeEntity.CHOOSE_ONE) {
                            // Currently 1-N is allowed
                            if (selectionsInThisSelection.length < 1) {
                                throw new RegistrationSelectionSubGroupSelectionAmountMismatchValidationException("Unable to process registration request." +
                                    " Subgroup selection amount mismatch, selection [" + sel.getTitle().toString() + "] has rule " + sel.getType() + " and has " +
                                    selectionsInThisSelection.length + " selections made, selections [" + StringUtils.arrayToCommaDelimitedString(selectionsInThisSelection) + "]." +
                                    " Failed realisation selection: " + getSelectionIdString(selection));
                            }
                        } else if (sel.getType() == SelectionTypeEntity.CHOOSE_MANY) {
                            // Currently anything (0-N) is allowed
                        }
                    }
                }
            } else {
                throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                        " Registration contains selection hierarchy for which root element is not type " +
                        RegistrationSelectionItemType.COURSE_UNIT + " or " + RegistrationSelectionItemType.REALISATION +
                        " Failed selection: " + getSelectionIdString(selection));
            }
        }
    }

    private ParentAwareRegistrationSelection findCourseUnitSelection(CreateRegistrationSelection selection) {
        ParentAwareRegistrationSelection current = selection;

        while(current != null) {
            if(current.getSelectionItemType() == RegistrationSelectionItemType.COURSE_UNIT) {
                return current;
            }

            current = current.getParent();
        }

        return null;
    }

    protected void verifyHierarchyReferences(CreateRegistrationSelection realisationSelection, CourseUnitEntity selectionCourseUnitEntity,
                                             RealisationEntity selectionRealisationEntity) {

        // Verify realisation selection parent is ASSESSMENT_ITEM
        if(realisationSelection == null || realisationSelection.getParent().getSelectionItemType() != RegistrationSelectionItemType.ASSESSMENT_ITEM) {
            throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                    " Registration contains realisation selection that has other parent " + RegistrationSelectionItemType.ASSESSMENT_ITEM + RegistrationSelectionItemType.COURSE_UNIT +
                    " Got: " + getSelectionIdString(realisationSelection.getParent()));
        }

        CreateRegistrationSelectionParent assessmentItemSelection = realisationSelection.getParent();

        // Verify selected realisation has reference to selected assessment item and course unit
        if(CollectionUtils.isEmpty(selectionRealisationEntity.getStudyElementReferences())
                || selectionRealisationEntity.getStudyElementReferences().stream().noneMatch(ref ->
                        ref.getReferenceIdentifier().equals(selectionCourseUnitEntity.getStudyElementId())
                        && ref.getReferenceOrganizer().equals(selectionCourseUnitEntity.getOrganizingOrganisationId())
                        && ref.getReferenceAssessmentItemId().equals(assessmentItemSelection.getSelectionItemId()))) {

            throw new RegistrationSelectionReferenceValidationException("Unable to process registration request." +
                    " Registration has selected realisation that does not have assessment item reference to given course unit." +
                    " Failed realisation selection: " + getSelectionIdString(realisationSelection) +
                    " Missing assessment item course unit [id: " + selectionCourseUnitEntity.getStudyElementId() + "," +
                    " code: " + selectionCourseUnitEntity.getStudyElementId() + "," +
                    " assessment item id: " + assessmentItemSelection.getSelectionItemId() + "]");
        }

        // Verify assessment item selection parent is COMPLETION_OPTION
        if(assessmentItemSelection.getParent() == null || assessmentItemSelection.getParent().getSelectionItemType() != RegistrationSelectionItemType.COMPLETION_OPTION) {
            throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                    " Registration contains realisation selection hierarchy in which assessment item does not have completion option parent." +
                    " Root realisation selection: " + getSelectionIdString(realisationSelection) +
                    " Incorrect parent: " + getSelectionIdString(assessmentItemSelection.getParent()));
        }

        CreateRegistrationSelectionParent completionOptionSelection = assessmentItemSelection.getParent();

        if(completionOptionSelection.getParent() == null || completionOptionSelection.getParent().getSelectionItemType() != RegistrationSelectionItemType.COURSE_UNIT) {
            throw new RegistrationSelectionHierarchyValidationException("Unable to process registration request." +
                    " Registration contains realisation selection hierarchy in which completion option does not have course unit parent." +
                    " Root realisation selection: " + getSelectionIdString(assessmentItemSelection) +
                    " Incorrect parent: " + getSelectionIdString(assessmentItemSelection.getParent()));
        }

        CreateRegistrationSelectionParent courseUnitSelection = completionOptionSelection.getParent();

        // Verify course unit has completion options -> if none found selection hierarchy must be wrong
        if(CollectionUtils.isEmpty(selectionCourseUnitEntity.getCompletionOptions())) {
            throw new RegistrationSelectionReferenceValidationException("Unable to process registration request." +
                    " Selected realisation's course unit does not contain given selection completion option " +
                    " Root realisation selection: " + getSelectionIdString(realisationSelection) +
                    " Completion option selection: " + getSelectionIdString(completionOptionSelection) +
                    " Course unit selection: " + getSelectionIdString(courseUnitSelection));
        }

        // Get selection course unit's completion option(s) that match given selected completion option
        if(StringUtils.hasText(completionOptionSelection.getSelectionItemId())) {
            List<CompletionOptionEntity> courseUnitCompletionOptionsMatchingSelectedCompletionOption = selectionCourseUnitEntity.getCompletionOptions().stream()
                .filter(co -> co.getCompletionOptionId().equals(completionOptionSelection.getSelectionItemId()))
                .toList();

            if (CollectionUtils.isEmpty(courseUnitCompletionOptionsMatchingSelectedCompletionOption)) {
                throw new RegistrationSelectionReferenceValidationException("Unable to process registration request." +
                    " Selected realisation's course unit does not contain given completion option." +
                    " Root realisation selection: " + getSelectionIdString(realisationSelection) +
                    " Completion option selection: " + getSelectionIdString(completionOptionSelection) +
                    " Course unit selection: " + getSelectionIdString(courseUnitSelection));
            }

            // Verify selection completion option contains given selected assessment item
            CompletionOptionEntity completionOptionContainingAssessmentItemSelection = courseUnitCompletionOptionsMatchingSelectedCompletionOption.stream()
                .filter(co -> !CollectionUtils.isEmpty(co.getAssessmentItems()))
                .filter(co -> co.getAssessmentItems().stream().anyMatch(ai -> ai.getAssessmentItemId().equals(assessmentItemSelection.getSelectionItemId())))
                .findFirst()
                .orElse(null);

            if (completionOptionContainingAssessmentItemSelection == null) {
                throw new RegistrationSelectionReferenceValidationException("Unable to process registration request." +
                    " Selected completion option does not contain given assessment item." +
                    " Root realisation selection: " + getSelectionIdString(realisationSelection) +
                    " Assessment item selection: " + getSelectionIdString(assessmentItemSelection) +
                    " Completion option selection: " + getSelectionIdString(completionOptionSelection) +
                    " Course unit selection: " + getSelectionIdString(courseUnitSelection));
            }
        }
    }

    protected boolean isOrganisationValidInNetwork(NetworkEntity networkEntity, String organisation) {
        return networkEntity.getOrganisations().stream().anyMatch(o -> o.getOrganisationTkCode().equals(organisation)
                && (o.getValidityInNetwork() != null)
                && (o.getValidityInNetwork().getStart() != null && DateUtils.isBeforeOrEqual(o.getValidityInNetwork().getStart(), OffsetDateTime.now()))
                && (o.getValidityInNetwork().getEnd() == null || DateUtils.isAfterOrEqual(o.getValidityInNetwork().getEnd(), OffsetDateTime.now())));
    }
}
