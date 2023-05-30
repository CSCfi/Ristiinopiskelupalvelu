package fi.uta.ristiinopiskelu.handler.helper;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.NetworkType;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.RegistrationSelectionItemType;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateCourseUnitRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyElementRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.request.CreateStudyModuleRequestDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class DtoInitializerV8 {
    public static CooperationNetwork getCooperationNetwork(String id, LocalisedString name, boolean enrollable, LocalDate validityStartDate, LocalDate validityEndDate) {
        CooperationNetwork cooperationNetwork = new CooperationNetwork();
        cooperationNetwork.setId(id);
        cooperationNetwork.setName(name);
        cooperationNetwork.setEnrollable(enrollable);
        cooperationNetwork.setValidityStartDate(validityStartDate);
        cooperationNetwork.setValidityEndDate(validityEndDate);
        return cooperationNetwork;
    }

    public static Organisation getOrganisation(String id, String tkCode) {
        Organisation organisation = new Organisation();
        organisation.setOrganisationIdentifier(id);
        organisation.setOrganisationTkCode(tkCode);
        return organisation;
    }

    public static OrganisationReference getOrganisationReference(Organisation organisation, OrganisationRole role) {
        OrganisationReference organisationReference = new OrganisationReference();
        organisationReference.setOrganisation(organisation);
        organisationReference.setPercent(new BigDecimal(100));
        organisationReference.setOrganisationRole(role);
        return organisationReference;
    }

    public static StudyElementReference getStudyElementReferenceForStudyModule(String referenceIdentifier, String referenceOrganizer) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementType.STUDY_MODULE, null);
    }

    public static StudyElementReference getStudyElementReferenceForCourseUnit(String referenceIdentifier, String referenceOrganizer) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementType.COURSE_UNIT, null);
    }

    public static StudyElementReference getStudyElementReferenceForAssessmentItem(String referenceIdentifier, String referenceOrganizer, String referenceAssessmentItemId) {
        return getStudyElementReference(referenceIdentifier, referenceOrganizer, StudyElementType.ASSESSMENT_ITEM, referenceAssessmentItemId);
    }

    private static StudyElementReference getStudyElementReference(String referenceIdentifier, String referenceOrganizer, StudyElementType type, String assessmentItemId) {
        StudyElementReference studyElementReference = new StudyElementReference();
        studyElementReference.setReferenceIdentifier(referenceIdentifier);
        studyElementReference.setReferenceOrganizer(referenceOrganizer);
        studyElementReference.setReferenceType(type);
        studyElementReference.setReferenceAssessmentItemId(assessmentItemId);
        return studyElementReference;
    }

    public static Realisation getRealisation(String realisationId, String realisationIdentifierCode, LocalisedString name, List<StudyElementReference> references,
                                 List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences) {
        Realisation realisation = new Realisation();
        realisation.setRealisationId(realisationId);
        realisation.setRealisationIdentifierCode(realisationIdentifierCode);
        realisation.setName(name);
        realisation.setStudyElementReferences(references);
        realisation.setCooperationNetworks(cooperationNetworks);
        realisation.setOrganisationReferences(organisationReferences);
        return realisation;
    }

    public static CourseUnit getCourseUnit(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                           List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                           BigDecimal minCredits, BigDecimal maxCredits) {
        return getStudyElement(studyElementId, studyElementIdentifierCode, name,
                cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.COURSE_UNIT, null, CourseUnit.class);
    }

    public static CreateCourseUnitRequestDTO getCreateCourseUnitRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                     List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                                     BigDecimal minCredits, BigDecimal maxCredits) {
        return getCreateStudyElementRequestDTO(studyElementId, studyElementIdentifierCode, name,
            cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.COURSE_UNIT, null, CreateCourseUnitRequestDTO.class);
    }

    public static StudyModule getStudyModule(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                             List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                             BigDecimal minCredits, BigDecimal maxCredits, List<StudyElement> subElements){
        StudyModule studyModule = getStudyElement(studyElementId, studyElementIdentifierCode, name,
                cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.STUDY_MODULE, subElements, StudyModule.class);
        studyModule.setOptionality(Optionality.MIN_MAX_CREDITS);
        return studyModule;
    }

    public static CreateStudyModuleRequestDTO getCreateStudyModuleRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                             List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                                             BigDecimal minCredits, BigDecimal maxCredits, List<CreateStudyElementRequestDTO> subElements){
        CreateStudyModuleRequestDTO studyModule = getCreateStudyElementRequestDTO(studyElementId, studyElementIdentifierCode, name,
            cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.STUDY_MODULE, subElements, CreateStudyModuleRequestDTO.class);
        studyModule.setOptionality(Optionality.MIN_MAX_CREDITS);
        return studyModule;
    }

    public static <T extends CreateStudyElementRequestDTO> T getCreateStudyElementRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                             List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                             BigDecimal minCredits, BigDecimal maxCredits, StudyElementType studyElementType,
                                                                             List<CreateStudyElementRequestDTO> subElements, Class<T> type) {
        T studyElement;
        try {
            studyElement = type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        studyElement.setStudyElementId(studyElementId);
        studyElement.setStudyElementIdentifierCode(studyElementIdentifierCode);
        studyElement.setName(name);
        studyElement.setCooperationNetworks(cooperationNetworks);
        studyElement.setOrganisationReferences(organisationReferences);
        studyElement.setSendingTime(OffsetDateTime.now());
        studyElement.setCreditsMin(minCredits);
        studyElement.setCreditsMax(maxCredits);
        studyElement.setSubElements(subElements);
        studyElement.setType(studyElementType);
        return studyElement;
    }

    public static <T extends StudyElement> T getStudyElement(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                              List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                              BigDecimal minCredits, BigDecimal maxCredits, StudyElementType studyElementType, List<StudyElement> subElements, Class<T> type) {
        T studyElement;
        try {
            studyElement = type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        studyElement.setStudyElementId(studyElementId);
        studyElement.setStudyElementIdentifierCode(studyElementIdentifierCode);
        studyElement.setName(name);
        studyElement.setCooperationNetworks(cooperationNetworks);
        studyElement.setOrganisationReferences(organisationReferences);
        studyElement.setSendingTime(OffsetDateTime.now());
        studyElement.setCreditsMin(minCredits);
        studyElement.setCreditsMax(maxCredits);
        studyElement.setSubElements(subElements);
        studyElement.setType(studyElementType);
        return studyElement;
    }

    private static RegistrationSelection getRegistrationSelection(String studyElementId, RegistrationSelectionItemType type,
                                                                 RegistrationSelectionItemStatus status, RegistrationSelection parent, List<String> subGroupSelections) {
        RegistrationSelection selection = new RegistrationSelection();
        selection.setSelectionItemId(studyElementId);
        selection.setSelectionItemType(type);
        selection.setSelectionItemStatus(status);
        selection.setParent(parent);
        selection.setSubGroupSelections(subGroupSelections);
        return selection;
    }

    public static RegistrationSelection getRegistrationSelectionCourseUnitParent(String studyElementId) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemType.COURSE_UNIT,
                null, null, null);
    }

    public static RegistrationSelection getRegistrationSelectionCourseUnit(String studyElementId, RegistrationSelectionItemStatus status) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemType.COURSE_UNIT,
                status, null, null);
    }

    public static RegistrationSelection getRegistrationSelectionRealisation(String studyElementId, RegistrationSelectionItemStatus status,
                                                                            RegistrationSelection parent, List<String> subGroupSelections) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemType.REALISATION,
                status, parent, subGroupSelections);
    }

    public static RegistrationSelection getRegistrationSelectionAssessmentItem(String studyElementId, RegistrationSelection parent) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemType.ASSESSMENT_ITEM,
                null, parent, null);
    }

    public static RegistrationSelection getRegistrationSelectionCompletionOption(String studyElementId, RegistrationSelection parent) {
        return getRegistrationSelection(studyElementId, RegistrationSelectionItemType.COMPLETION_OPTION,
                null, parent, null);
    }

    public static Selection getSelection(LocalisedString title, SelectionType type, List<SelectionValue> selectionValues) {
        Selection groupSelection = new Selection();
        groupSelection.setTitle(title);
        groupSelection.setType(type);
        groupSelection.setSelectionValues(selectionValues);
        return groupSelection;
    }

    public static Person getPerson(String firstnames, String surname, String homeEppn, String hostEppn) {
        Person person = new Person();
        person.setFirstNames(firstnames);
        person.setSurName(surname);
        person.setHomeEppn(homeEppn);
        person.setHostEppn(hostEppn);
        return person;
    }

    public static CompletedCreditAssessment getCompletedCreditAssessment(String description, String egt, ScaleValue scale, GradeCode gradeCode) {
        GradingScale gradingScale = new GradingScale();
        gradingScale.setScale(scale);

        Grade grade = new Grade();
        grade.setCode(gradeCode);

        CompletedCreditAssessment completedCreditAssessment = new CompletedCreditAssessment();
        completedCreditAssessment.setDescription(description);
        completedCreditAssessment.setEgt(egt);
        completedCreditAssessment.setGrade(grade);
        completedCreditAssessment.setGradingScale(gradingScale);
        return completedCreditAssessment;
    }

    public static StudyRecordOrganisation getStudyRecordOrganisation(String organisationId, String organisationTkCode, LocalisedString name) {
        StudyRecordOrganisation organisation = new StudyRecordOrganisation();
        organisation.setOrganisationIdentifier("org-2");
        organisation.setOrganisationName(new LocalisedString("Nimen kuvaus", null, null));
        organisation.setOrganisationTkCode("123456");
        organisation.setErasmusOrganisationName("Nimen kuvaus");
        return organisation;
    }

    public static CompletedCreditTarget getCompletedCreditTarget(String ccTargetId, String ccTargetIdentifierCode, CompletedCreditTargetType type) {
        CompletedCreditTarget completedCreditTarget = new CompletedCreditTarget();
        completedCreditTarget.setCompletedCreditTargetId(ccTargetId);
        completedCreditTarget.setCompletedCreditTargetIdentifierCode(ccTargetIdentifierCode);
        completedCreditTarget.setCompletedCreditTargetType(type);
        return completedCreditTarget;
    }

    public static CompletedCredit getCompletedCreditForCourseUnit(String ccIdentifier, LocalisedString name, CompletedCreditStatus status, CompletedCreditTarget target,
                                                                  CompletedCreditType type, List<Person> acceptors, CompletedCreditAssessment assessment,
                                                                  String educationInstitution, StudyRecordOrganisation organisation) {
        return getCompletedCredit(ccIdentifier, name, status, target, type, acceptors, assessment,null, educationInstitution, organisation);
    }

    private static CompletedCredit getCompletedCredit(String ccIdentifier, LocalisedString name, CompletedCreditStatus status, CompletedCreditTarget target,
                                                     CompletedCreditType type, List<Person> acceptors, CompletedCreditAssessment assessment,
                                                     List<CompletedCreditAssociation> associations, String educationInstitution, StudyRecordOrganisation organisation) {
        CompletedCredit completedCredit = new CompletedCredit();
        completedCredit.setCompletedCreditIdentifier(ccIdentifier);
        completedCredit.setAcceptors(acceptors);
        completedCredit.setAssessment(assessment);
        completedCredit.setCompletionDate(LocalDate.of(2019, 7, 11));
        completedCredit.setCompletedCreditAssociations(associations);
        completedCredit.setCompletedCreditStatus(status);
        completedCredit.setCompletedCreditTarget(target);
        completedCredit.setCompletedCreditName(name);
        completedCredit.setLanguagesOfCompletion(Collections.singletonList("fi"));
        completedCredit.setMinEduGuidanceArea(MinEduGuidanceArea.EDUCATION);
        completedCredit.setEducationInstitution(educationInstitution); // suorituksesta vastaava myöntävä organisaatio
        completedCredit.setOrganisationResponsibleForCompletion(organisation); // Miten tämä eroaa ylemmästä?
        completedCredit.setScope(5.0); // laajuus
        completedCredit.setTransaction(CompletedCreditTransaction.NEW_COMPLETED_CREDIT);
        completedCredit.setType(type);
        return completedCredit;
    }

    public static Network getNetwork(String id, LocalisedString name, Validity validity, List<NetworkOrganisation> networkOrganisations) {
        Network network = new Network();
        network.setId(id);
        network.setName(name);
        network.setOrganisations(networkOrganisations);
        network.setValidity(validity);
        network.setNetworkType(NetworkType.CURRICULUM_LEVEL);
        network.setPublished(true);
        return network;
    }

    public static CompletionOption getCompletionOption(String id, String description, List<AssessmentItem> assessmentItems, LocalisedString name) {
        CompletionOption completionOption = new CompletionOption();
        completionOption.setCompletionOptionId(id);
        completionOption.setDescription(description);
        completionOption.setAssessmentItems(assessmentItems);
        completionOption.setName(name);
        return completionOption;
    }

    public static AssessmentItem getAssessmentItem(String id, LocalisedString name) {
        AssessmentItem assessmentItem = new AssessmentItem();
        assessmentItem.setAssessmentItemId(id);
        assessmentItem.setName(name);
        return assessmentItem;
    }

    public static Validity getIndefinitelyValidity(OffsetDateTime start) {
        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.INDEFINITELY);
        validity.setStart(start);
        return validity;
    }

    public static Validity getFixedValidity(OffsetDateTime start, OffsetDateTime end) {
        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(start);
        validity.setEnd(end);
        return validity;
    }

    public static GroupQuota getGroupQuota(List<String> networkIds, Integer minSeats, Integer maxSeats) {
        GroupQuota groupQuota = new GroupQuota();
        groupQuota.setNetworkIdentifiers(networkIds);
        groupQuota.setMinSeats(minSeats);
        groupQuota.setMaxSeats(maxSeats);
        return groupQuota;
    }

    public static StudyRight getStudyRight(String organisationId) {
        StudyRightIdentifier studyRightIdentifier = new StudyRightIdentifier();
        studyRightIdentifier.setStudyRightId("OPISKOIK1");
        studyRightIdentifier.setOrganisationTkCodeReference(organisationId);

        StudyRightStatus studyRightStatus = new StudyRightStatus();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ACTIVE);
        studyRightStatus.setStartDate(LocalDate.of(2017, 1, 1));
        studyRightStatus.setEndDate(LocalDate.of(2020, 6, 1));

        StudyRight homeStudyRight = new StudyRight();
        homeStudyRight.setIdentifiers(studyRightIdentifier);
        homeStudyRight.setStudyRightStatus(studyRightStatus);

        return homeStudyRight;
    }

    public static StudentStudyRight getStudentStudyRight(String organisationId, boolean eligibleForNetworkStudies) {
        StudyRight studyRight = DtoInitializerV8.getStudyRight(organisationId);
        StudentStudyRight studentStudyRight = new StudentStudyRight();
        studentStudyRight.setStudyRightType(studyRight.getStudyRightType());
        studentStudyRight.setStudyRightStatus(studyRight.getStudyRightStatus());
        studentStudyRight.setKeywords(studyRight.getKeywords());
        studentStudyRight.setIdentifiers(studyRight.getIdentifiers());
        studentStudyRight.setEligibleForNetworkStudies(eligibleForNetworkStudies);
        return studentStudyRight;
    }
}
