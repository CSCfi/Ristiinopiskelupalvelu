package fi.uta.ristiinopiskelu.handler.helper;

import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.messaging.util.Oid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class EntityInitializer {
    public static CourseUnitEntity getCourseUnitEntityWithParents(String id, String code, String organisingOrganisationId, List<CooperationNetwork> cooperationNetworks,
                                                                  LocalisedString name, List<StudyElementReference> parents) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        courseUnitEntity.setParents(parents);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntityWithCompletionOptions(String id, String code, String organisingOrganisationId, List<CooperationNetwork> cooperationNetworks,
                                                                            LocalisedString name, List<CompletionOptionEntity> completionOptions) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        courseUnitEntity.setCompletionOptions(completionOptions);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntity(String id, String organisingOrganisationId, List<CooperationNetwork> cooperationNetworks,
                                                       LocalisedString name) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, null, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        return courseUnitEntity;
    }

    public static CourseUnitEntity getCourseUnitEntity(String id, String code, String organisingOrganisationId, List<CooperationNetwork> cooperationNetworks,
                                                       LocalisedString name) {
        CourseUnitEntity courseUnitEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, CourseUnitEntity.class);
        return courseUnitEntity;
    }

    public static StudyModuleEntity getStudyModuleEntity(String id, String code, String organisingOrganisationId, List<CooperationNetwork> cooperationNetworks,
                                                         LocalisedString name) {
        StudyModuleEntity studyModuleEntity = getStudyElementEntity(id, code, name, cooperationNetworks, organisingOrganisationId, StudyModuleEntity.class);
        return studyModuleEntity;
    }

    public static StudyRecordEntity getStudyRecordEntity(String sendingOrganisation, String receivingOrganisation, StudyRecordStudent student, CompletedCredit... completedCredits) {
        StudyRecordEntity studyRecord = new StudyRecordEntity();
        studyRecord.setStatus(StudyRecordStatus.RECORDED);
        studyRecord.setStudent(student);
        studyRecord.setSendingTime(OffsetDateTime.now());
        studyRecord.setReceivingOrganisation(receivingOrganisation);
        studyRecord.setSendingOrganisation(sendingOrganisation);
        studyRecord.setRoutingType(RoutingType.OTHER);
        studyRecord.setCompletedCredits(Arrays.asList(completedCredits));
        return studyRecord;
    }

    private static <T extends StudyElementEntity> T getStudyElementEntity(String studyElementId, String studyElementIdentifierCode, LocalisedString name,
                                                                          List<CooperationNetwork> cooperationNetworks, String organizingOrganisationId, Class<T> type) {
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
        studyElementEntity.setStatus(StudyStatus.ACTIVE);
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
        completionOptionEntity.setDescription("Testi completion option");
        completionOptionEntity.setAssessmentItems(assessmentItemEntities);
        return completionOptionEntity;
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String realisationIdentifierCode, String organizingOrganisationId,
                                                         List<StudyElementReference> references, List<CooperationNetwork> cooperationNetwork, List<Selection> groupSelections) {
        RealisationEntity realisationEntity = new RealisationEntity();
        realisationEntity.setRealisationId(realisationId);
        realisationEntity.setRealisationIdentifierCode(realisationIdentifierCode);
        realisationEntity.setOrganizingOrganisationId(organizingOrganisationId);
        realisationEntity.setStudyElementReferences(references);
        realisationEntity.setCooperationNetworks(cooperationNetwork);
        realisationEntity.setGroupSelections(groupSelections);
        realisationEntity.setStatus(StudyStatus.ACTIVE);
        return realisationEntity;
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String organizingOrganisationId,
                                                         List<StudyElementReference> references, List<CooperationNetwork> cooperationNetwork) {
        return getRealisationEntity(realisationId, null, organizingOrganisationId,
                references, cooperationNetwork, null);
    }

    public static RealisationEntity getRealisationEntity(String realisationId, String realisationIdentifierCode, String organizingOrganisationId,
                                                         List<StudyElementReference> references, List<CooperationNetwork> cooperationNetwork) {
        return getRealisationEntity(realisationId, realisationIdentifierCode, organizingOrganisationId,
                references, cooperationNetwork, null);
    }

    public static OrganisationEntity getOrganisationEntity(String id, String queue, LocalisedString name, int schemaVersion) {
        OrganisationEntity sendingOrganisation = new OrganisationEntity();
        sendingOrganisation.setId(id);
        sendingOrganisation.setQueue(queue);
        sendingOrganisation.setOrganisationName(name);
        sendingOrganisation.setSchemaVersion(schemaVersion);
        return sendingOrganisation;
    }

    public static NetworkEntity getNetworkEntity(String id, LocalisedString name, List<NetworkOrganisation> organisations, Validity validity, boolean published) {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId(id);
        networkEntity.setName(name);
        networkEntity.setOrganisations(organisations);
        networkEntity.setValidity(validity);
        networkEntity.setPublished(published);
        return networkEntity;
    }

    public static RegistrationEntity getRegistrationEntity(String sendingOrganisationId, String receivingOrganisationId,
                                                           List<RegistrationSelection> selections, RegistrationStatus status,
                                                           String networkIdentifier) {
        return getRegistrationEntity(sendingOrganisationId, receivingOrganisationId, selections, null, status, networkIdentifier);
    }

    public static RegistrationEntity getRegistrationEntity(String sendingOrganisationId, String receivingOrganisationId,
                                                           List<RegistrationSelection> selections, List<RegistrationSelection> selectionsReplies,
                                                           RegistrationStatus status, String networkIdentifier) {
        StudyRight homeStudyRight = DtoInitializer.getStudyRight(sendingOrganisationId);
        StudyRight hostStudyRight = DtoInitializer.getStudyRight(receivingOrganisationId);

        ExtendedStudent student = new ExtendedStudent();
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        student.setPersonId(HetuUtil.generateRandom());
        student.setHomeEppn("mactestington@eppn.fi");
        student.setHomeStudentNumber("1234567");
        student.setFirstNames("Testi");
        student.setSurName("Mac Testington");
        student.setGivenName("Testo");
        student.setHostStudentNumber("1234566");
        student.setHostEppn("testst@testi2.fi");
        student.setHomeStudyRight(homeStudyRight);
        student.setHostStudyRight(hostStudyRight);

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

    public static CourseUnitRealisationEntity getCourseUnitRealisation(String id, String identifierCode, String organizingOrganisationId, LocalisedString name, LocalDate startDate,
                                                                       LocalDate endDate, OffsetDateTime enrollmentStartDateTime, OffsetDateTime enrollmentEndDateTime) {
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
}
