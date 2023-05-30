package fi.uta.ristiinopiskelu.handler.helper;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CompletionOptionWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class DtoInitializer {
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

    public static RealisationWriteDTO getRealisation(String realisationId, String realisationIdentifierCode, LocalisedString name, List<StudyElementReference> references,
                                                     List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences) {
        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setRealisationId(realisationId);
        realisation.setRealisationIdentifierCode(realisationIdentifierCode);
        realisation.setName(name);
        realisation.setStudyElementReferences(references);
        realisation.setCooperationNetworks(cooperationNetworks);
        realisation.setOrganisationReferences(organisationReferences);
        return realisation;
    }

    public static CourseUnitWriteDTO getCourseUnit(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                           List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                           BigDecimal minCredits, BigDecimal maxCredits) {
        return getStudyElement(studyElementId, studyElementIdentifierCode, name,
                cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.COURSE_UNIT, null, CourseUnitWriteDTO.class);
    }

    public static CourseUnitWriteDTO getCreateCourseUnitRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                   List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                                   BigDecimal minCredits, BigDecimal maxCredits) {
        return getCreateStudyElementRequestDTO(studyElementId, studyElementIdentifierCode, name,
            cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.COURSE_UNIT, null, CourseUnitWriteDTO.class);
    }

    public static StudyModuleWriteDTO getStudyModule(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                             List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                             BigDecimal minCredits, BigDecimal maxCredits, List<AbstractStudyElementWriteDTO> subElements){
        StudyModuleWriteDTO studyModule = getStudyElement(studyElementId, studyElementIdentifierCode, name,
                cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.STUDY_MODULE, subElements, StudyModuleWriteDTO.class);
        studyModule.setOptionality(Optionality.MIN_MAX_CREDITS);
        return studyModule;
    }

    public static StudyModuleWriteDTO getCreateStudyModuleRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                     List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                                     BigDecimal minCredits, BigDecimal maxCredits, List<AbstractStudyElementWriteDTO> subElements){
        StudyModuleWriteDTO studyModule = getCreateStudyElementRequestDTO(studyElementId, studyElementIdentifierCode, name,
            cooperationNetworks, organisationReferences, minCredits, maxCredits, StudyElementType.STUDY_MODULE, subElements, StudyModuleWriteDTO.class);
        studyModule.setOptionality(Optionality.MIN_MAX_CREDITS);
        return studyModule;
    }

    public static <T extends AbstractStudyElementWriteDTO> T getCreateStudyElementRequestDTO(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                                             List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                                                             BigDecimal minCredits, BigDecimal maxCredits, StudyElementType studyElementType,
                                                                                             List<AbstractStudyElementWriteDTO> subElements, Class<T> type) {
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

    public static <T extends AbstractStudyElementWriteDTO> T getStudyElement(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                              List<CooperationNetwork> cooperationNetworks, List<OrganisationReference> organisationReferences,
                                                              BigDecimal minCredits, BigDecimal maxCredits, StudyElementType studyElementType, List<AbstractStudyElementWriteDTO> subElements, Class<T> type) {
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

    public static CompletedCredit getCompletedCredit(String identifier, String completedCreditTargetId,
                                                     CompletedCreditTargetType completedCreditTargetType,
                                                     MinEduGuidanceArea minEduGuidanceArea,
                                                     LocalisedString completedCreditName) {
        CompletedCreditTarget target = new CompletedCreditTarget();
        target.setCompletedCreditTargetId(completedCreditTargetId);
        target.setCompletedCreditTargetType(completedCreditTargetType);

        CompletedCredit completedCredit = new CompletedCredit();
        completedCredit.setCompletedCreditIdentifier(identifier);
        completedCredit.setCompletedCreditTarget(target);
        completedCredit.setMinEduGuidanceArea(minEduGuidanceArea);
        completedCredit.setCompletedCreditName(completedCreditName);

        return completedCredit;
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
        organisation.setErasmusOrganisationName("Orkanisaatio");
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

    public static NetworkWriteDTO getNetwork(String id, LocalisedString name, Validity validity, List<NetworkOrganisation> networkOrganisations) {
        NetworkWriteDTO network = new NetworkWriteDTO();
        network.setId(id);
        network.setName(name);
        network.setOrganisations(networkOrganisations);
        network.setValidity(validity);
        network.setNetworkType(NetworkType.CURRICULUM_LEVEL);
        network.setPublished(true);
        return network;
    }

    public static CompletionOptionWriteDTO getCompletionOption(String id, String description, List<AssessmentItemWriteDTO> assessmentItems, LocalisedString name) {
        CompletionOptionWriteDTO completionOption = new CompletionOptionWriteDTO();
        completionOption.setCompletionOptionId(id);
        completionOption.setDescription(description);
        completionOption.setAssessmentItems(assessmentItems);
        completionOption.setName(name);
        return completionOption;
    }

    public static AssessmentItemWriteDTO getAssessmentItem(String id, LocalisedString name) {
        AssessmentItemWriteDTO assessmentItem = new AssessmentItemWriteDTO();
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
        StudyRight studyRight = DtoInitializer.getStudyRight(organisationId);
        StudentStudyRight studentStudyRight = new StudentStudyRight();
        studentStudyRight.setStudyRightType(studyRight.getStudyRightType());
        studentStudyRight.setStudyRightStatus(studyRight.getStudyRightStatus());
        studentStudyRight.setKeywords(studyRight.getKeywords());
        studentStudyRight.setIdentifiers(studyRight.getIdentifiers());
        studentStudyRight.setEligibleForNetworkStudies(eligibleForNetworkStudies);
        return studentStudyRight;
    }
}
