package fi.uta.ristiinopiskelu.handler.helper;

import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.RoutingTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.StudyRecordStatusEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EntityInitializer {
    public static CourseUnitEntity getCourseUnitEntityWithParents(String id, String code, String organisingOrganisationId, List<CooperationNetworkEntity> cooperationNetworks,
                                                                  LocalisedStringEntity name, List<StudyElementReferenceEntity> parents) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        courseUnitEntity.setParents(parents);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntityWithCompletionOptions(String id, String code, String organisingOrganisationId, List<CooperationNetworkEntity> cooperationNetworks,
                                                                            LocalisedStringEntity name, List<CompletionOptionEntity> completionOptions) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        courseUnitEntity.setCompletionOptions(completionOptions);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntity(String id, String organisingOrganisationId, List<CooperationNetworkEntity> cooperationNetworks,
                                                       LocalisedStringEntity name) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, null, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntity(String id, String code, String organisingOrganisationId, List<CooperationNetworkEntity> cooperationNetworks,
                                                       LocalisedStringEntity name) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        return courseUnitEntity;
    }

    public static StudyModuleEntity getStudyModuleEntity(String id, String code, String organisingOrganisationId, List<CooperationNetworkEntity> cooperationNetworks,
                                                         LocalisedStringEntity name) {
        StudyModuleEntity studyModuleEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, StudyModuleEntity.class);
        return studyModuleEntity;
    }

    public static StudyRecordEntity getStudyRecordEntity(String sendingOrganisation, String receivingOrganisation, StudyRecordStudentEntity student, CompletedCreditEntity... completedCredits) {
        StudyRecordEntity studyRecord = new StudyRecordEntity();
        studyRecord.setStatus(StudyRecordStatusEntity.RECORDED);
        studyRecord.setStudent(student);
        studyRecord.setSendingTime(OffsetDateTime.now());
        studyRecord.setReceivingOrganisation(receivingOrganisation);
        studyRecord.setSendingOrganisation(sendingOrganisation);
        studyRecord.setRoutingType(RoutingTypeEntity.OTHER);
        studyRecord.setCompletedCredits(Arrays.asList(completedCredits));
        return studyRecord;
    }

    private static <T extends StudyElementEntity> T getStudyElementEntity(String studyElementId, String studyElementIdentifierCode, LocalisedStringEntity name,
                                                                          List<CooperationNetworkEntity> cooperationNetworks, String organizingOrganisationId, Class<T> type) {
        T studyElementEntity;
        try {
            studyElementEntity = type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        studyElementEntity.setStudyElementId(studyElementId);
        studyElementEntity.setStudyElementIdentifierCode(studyElementIdentifierCode);
        studyElementEntity.setName(name);
        studyElementEntity.setCooperationNetworks(cooperationNetworks);
        studyElementEntity.setSendingTime(OffsetDateTime.now());
        studyElementEntity.setOrganizingOrganisationId(organizingOrganisationId);
        studyElementEntity.setOrganisationReferences(getDefaultOrganisationReferences(organizingOrganisationId));
        studyElementEntity.setStatus(StudyStatusEntity.ACTIVE);
        return studyElementEntity;
    }

    public static AssessmentItemEntity getAssessmentItemEntity(String id, String primaryCourseUnitId) {
        AssessmentItemEntity assessmentItemEntity = new AssessmentItemEntity();
        assessmentItemEntity.setAssessmentItemId(id);
        assessmentItemEntity.setPrimaryCourseUnitId(primaryCourseUnitId);
        assessmentItemEntity.setCreditsMin(new BigDecimal(5));
        assessmentItemEntity.setCreditsMax(new BigDecimal(5));
        return assessmentItemEntity;
    }

    public static CompletionOptionEntity getCompletionOptionEntity(String id, List<AssessmentItemEntity> assessmentItemEntities) {
        CompletionOptionEntity completionOptionEntity = new CompletionOptionEntity();
        completionOptionEntity.setCompletionOptionId(id);
        completionOptionEntity.setDescription(new LocalisedStringEntity("Testi completion option", null, null));
        completionOptionEntity.setAssessmentItems(assessmentItemEntities);
        return completionOptionEntity;
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String realisationIdentifierCode, String organizingOrganisationId,
                                                         List<StudyElementReferenceEntity> references, List<CooperationNetworkEntity> cooperationNetwork, List<SelectionEntity> groupSelections) {
        RealisationEntity realisationEntity = new RealisationEntity();
        realisationEntity.setRealisationId(realisationId);
        realisationEntity.setRealisationIdentifierCode(realisationIdentifierCode);
        realisationEntity.setOrganizingOrganisationId(organizingOrganisationId);
        realisationEntity.setStudyElementReferences(references);
        realisationEntity.setCooperationNetworks(cooperationNetwork);
        realisationEntity.setGroupSelections(groupSelections);
        realisationEntity.setOrganisationReferences(getDefaultOrganisationReferences(organizingOrganisationId));
        realisationEntity.setStatus(StudyStatusEntity.ACTIVE);
        return realisationEntity;
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String organizingOrganisationId,
                                                         List<StudyElementReferenceEntity> references, List<CooperationNetworkEntity> cooperationNetwork) {
        return getRealisationEntity(realisationId, null, organizingOrganisationId,
                references, cooperationNetwork, null);
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String realisationIdentifierCode, String organizingOrganisationId,
                                                         List<StudyElementReferenceEntity> references, List<CooperationNetworkEntity> cooperationNetwork) {
        return getRealisationEntity(realisationId, realisationIdentifierCode, organizingOrganisationId,
                references, cooperationNetwork, null);
    }

    public static OrganisationEntity getOrganisationEntity(String id, String queue, LocalisedStringEntity name, int schemaVersion) {
        OrganisationEntity sendingOrganisation = new OrganisationEntity();
        sendingOrganisation.setId(id);
        sendingOrganisation.setQueue(queue);
        sendingOrganisation.setOrganisationName(name);
        sendingOrganisation.setSchemaVersion(schemaVersion);
        return sendingOrganisation;
    }

    public static NetworkEntity getNetworkEntity(String id, LocalisedStringEntity name, List<NetworkOrganisationEntity> organisations,
                                                 ValidityEntity validity, boolean published) {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId(id);
        networkEntity.setName(name);
        networkEntity.setOrganisations(organisations);
        networkEntity.setNetworkType(NetworkTypeEntity.CURRICULUM_LEVEL);
        networkEntity.setValidity(validity);
        networkEntity.setPublished(published);
        return networkEntity;
    }

    public static RegistrationEntity getRegistrationEntity(String sendingOrganisationId, String receivingOrganisationId,
                                                           List<CreateRegistrationSelectionEntity> selections, RegistrationStatusEntity status,
                                                           String networkIdentifier) {
        return getRegistrationEntity(sendingOrganisationId, receivingOrganisationId, selections, null, status, networkIdentifier);
    }

    public static RegistrationEntity getRegistrationEntity(String sendingOrganisationId, String receivingOrganisationId,
                                                           List<CreateRegistrationSelectionEntity> selections, List<RegistrationReplySelectionEntity> selectionsReplies,
                                                           RegistrationStatusEntity status, String networkIdentifier) {
        StudyRightEntity homeStudyRight = getStudyRight(sendingOrganisationId);
        StudyRightEntity hostStudyRight = getStudyRight(receivingOrganisationId);

        RegistrationStudentEntity student = RegistrationStudentEntity.builder()
            .homeStudentNumber("1234567")
            .hostStudentNumber("1234566")
            .homeStudyRight(homeStudyRight)
            .hostStudyRight(hostStudyRight)
            .build();

        RegistrationEntity registration = new RegistrationEntity();
        registration.setReceivingOrganisationTkCode(receivingOrganisationId);
        registration.setSendingOrganisationTkCode(sendingOrganisationId);
        registration.setStudent(student);
        registration.setSelections(selections);
        registration.setSelectionsReplies(selectionsReplies);
        registration.setStatus(status);
        registration.setNetworkIdentifier(networkIdentifier);
        registration.setEnrolmentDateTime(OffsetDateTime.now());
        return registration;
    }

    public static StudyRightEntity getStudyRight(String organisationId) {
        StudyRightIdentifierEntity studyRightIdentifier = new StudyRightIdentifierEntity();
        studyRightIdentifier.setStudyRightId("OPISKOIK1");
        studyRightIdentifier.setOrganisationTkCodeReference(organisationId);

        StudyRightStatusEntity studyRightStatus = new StudyRightStatusEntity();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValueEntity.ACTIVE);
        studyRightStatus.setStartDate(LocalDate.of(2017, 1, 1));
        studyRightStatus.setEndDate(LocalDate.of(2020, 6, 1));

        StudyRightEntity studyRight = new StudyRightEntity();
        studyRight.setIdentifiers(studyRightIdentifier);
        studyRight.setStudyRightStatus(studyRightStatus);
        return studyRight;
    }

    private static CreateRegistrationSelectionEntity getRegistrationSelection(String studyElementId, RegistrationSelectionItemTypeEntity type,
                                                                              RegistrationSelectionItemStatusEntity status,
                                                                              CreateRegistrationSelectionParentEntity parent, List<String> subGroupSelections) {
        CreateRegistrationSelectionEntity selection = new CreateRegistrationSelectionEntity();
        selection.setSelectionItemId(studyElementId);
        selection.setSelectionItemType(type);
        selection.setSelectionItemStatus(status);
        selection.setParent(parent);
        selection.setSubGroupSelections(subGroupSelections);
        return selection;
    }

    private static RegistrationReplySelectionEntity getRegistrationReplySelection(String studyElementId, RegistrationSelectionItemTypeEntity type,
                                                                                  RegistrationSelectionItemStatusEntity status,
                                                                                  List<String> subGroupSelections) {
        RegistrationReplySelectionEntity selection = new RegistrationReplySelectionEntity();
        selection.setSelectionItemId(studyElementId);
        selection.setSelectionItemType(type);
        selection.setSelectionItemStatus(status);
        selection.setSubGroupSelections(subGroupSelections);
        return selection;
    }

    private static CreateRegistrationSelectionParentEntity getRegistrationSelectionParent(String studyElementId, RegistrationSelectionItemTypeEntity type,
                                                                                          CreateRegistrationSelectionParentEntity parent) {
        CreateRegistrationSelectionParentEntity selection = new CreateRegistrationSelectionParentEntity();
        selection.setSelectionItemId(studyElementId);
        selection.setSelectionItemType(type);
        selection.setParent(parent);
        return selection;
    }

    public static CreateRegistrationSelectionParentEntity getRegistrationSelectionCourseUnitParent(String studyElementId) {
        return getRegistrationSelectionParent(studyElementId, RegistrationSelectionItemTypeEntity.COURSE_UNIT, null);
    }

    public static CreateRegistrationSelectionEntity getRegistrationSelectionCourseUnit(String studyElementId, RegistrationSelectionItemStatusEntity status) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemTypeEntity.COURSE_UNIT, status, null, null);
    }

    public static CreateRegistrationSelectionEntity getRegistrationSelectionRealisation(String studyElementId, RegistrationSelectionItemStatusEntity status,
                                                                                        CreateRegistrationSelectionParentEntity parent, List<String> subGroupSelections) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemTypeEntity.REALISATION, status, parent, subGroupSelections);
    }

    public static RegistrationReplySelectionEntity getRegistrationReplySelectionRealisation(String studyElementId, RegistrationSelectionItemStatusEntity status,
                                                                                            List<String> subGroupSelections) {
        return getRegistrationReplySelection(studyElementId, RegistrationSelectionItemTypeEntity.REALISATION, status, subGroupSelections);
    }

    public static RegistrationReplySelectionEntity getRegistrationReplySelectionCourseUnit(String studyElementId, RegistrationSelectionItemStatusEntity status) {
        return getRegistrationReplySelection(studyElementId, RegistrationSelectionItemTypeEntity.COURSE_UNIT, status, null);
    }

    public static SelectionEntity getSelection(LocalisedStringEntity title, SelectionTypeEntity type, List<SelectionValueEntity> selectionValues) {
        SelectionEntity selection = new SelectionEntity();
        selection.setTitle(title);
        selection.setType(type);
        selection.setSelectionValues(selectionValues);
        return selection;
    }

    public static CourseUnitRealisationEntity getCourseUnitRealisation(String id, String identifierCode, String organizingOrganisationId,
                                                                       LocalisedStringEntity name, LocalDate startDate,
                                                                       LocalDate endDate, OffsetDateTime enrollmentStartDateTime,
                                                                       OffsetDateTime enrollmentEndDateTime) {
        CourseUnitRealisationEntity cur = new CourseUnitRealisationEntity();
        cur.setRealisationId(id);
        cur.setOrganizingOrganisationId(organizingOrganisationId);
        cur.setStartDate(startDate);
        cur.setEndDate(endDate);
        cur.setEnrollmentStartDateTime(enrollmentStartDateTime);
        cur.setEnrollmentEndDateTime(enrollmentEndDateTime);
        cur.setName(name);
        cur.setRealisationIdentifierCode(identifierCode);
        return cur;
    }

    public static OrganisationReferenceEntity getOrganisationReference(OrganisationDataEntity organisation, OrganisationRoleEntity role) {
        OrganisationReferenceEntity organisationReference = new OrganisationReferenceEntity();
        organisationReference.setTarget(organisation);
        organisationReference.setPercent(new BigDecimal(100));
        organisationReference.setOrganisationRole(role);
        return organisationReference;
    }

    public static OrganisationDataEntity getOrganisation(String id, String tkCode) {
        OrganisationDataEntity organisation = new OrganisationDataEntity();
        organisation.setOrganisationIdentifier(id);
        organisation.setOrganisationTkCode(tkCode);
        return organisation;
    }

    public static CooperationNetworkEntity getCooperationNetwork(String id, LocalisedStringEntity name, boolean enrollable,
                                                                 LocalDate validityStartDate, LocalDate validityEndDate) {
        CooperationNetworkEntity cooperationNetwork = new CooperationNetworkEntity();
        cooperationNetwork.setId(id);
        cooperationNetwork.setName(name);
        cooperationNetwork.setEnrollable(enrollable);
        cooperationNetwork.setValidityStartDate(validityStartDate);
        cooperationNetwork.setValidityEndDate(validityEndDate);
        return cooperationNetwork;
    }

    public static StudyElementReferenceEntity getStudyElementReferenceForStudyModule(String referenceIdentifier, String referenceOrganizer) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementTypeEntity.STUDY_MODULE, null);
    }

    public static StudyElementReferenceEntity getStudyElementReferenceForCourseUnit(String referenceIdentifier, String referenceOrganizer) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementTypeEntity.COURSE_UNIT, null);
    }

    public static StudyElementReferenceEntity getStudyElementReferenceForAssessmentItem(String referenceIdentifier, String referenceOrganizer, String referenceAssessmentItemId) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementTypeEntity.ASSESSMENT_ITEM, referenceAssessmentItemId);
    }

    private static StudyElementReferenceEntity getStudyElementReference(String referenceIdentifier, String referenceOrganizer,
                                                                        StudyElementTypeEntity type, String assessmentItemId) {
        StudyElementReferenceEntity studyElementReference = new StudyElementReferenceEntity();
        studyElementReference.setReferenceIdentifier(referenceIdentifier);
        studyElementReference.setReferenceOrganizer(referenceOrganizer);
        studyElementReference.setReferenceType(type);
        studyElementReference.setReferenceAssessmentItemId(assessmentItemId);
        return studyElementReference;
    }

    public static ValidityEntity getIndefinitelyValidity(OffsetDateTime start) {
        ValidityEntity validity = new ValidityEntity();
        validity.setContinuity(ValidityEntity.ContinuityEntity.INDEFINITELY);
        validity.setStart(start);
        return validity;
    }

    public static ValidityEntity getFixedValidity(OffsetDateTime start, OffsetDateTime end) {
        ValidityEntity validity = new ValidityEntity();
        validity.setContinuity(ValidityEntity.ContinuityEntity.FIXED);
        validity.setStart(start);
        validity.setEnd(end);
        return validity;
    }

    private static List<OrganisationReferenceEntity> getDefaultOrganisationReferences(String organizingOrganisationId) {
        if(organizingOrganisationId == null) {
            return null;
        }

        return Collections.singletonList(getOrganisationReference(
            getOrganisation(organizingOrganisationId, organizingOrganisationId),
            OrganisationRoleEntity.ROLE_MAIN_ORGANIZER));
    }
}
