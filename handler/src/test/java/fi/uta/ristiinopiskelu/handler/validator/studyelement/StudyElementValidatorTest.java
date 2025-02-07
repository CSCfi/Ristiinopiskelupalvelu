package fi.uta.ristiinopiskelu.handler.validator.studyelement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.impl.AbstractStudyElementService;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule.CreateStudyModuleValidatorTest;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class StudyElementValidatorTest {

    @MockBean
    private AbstractStudyElementService abstractStudyElementService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private Validator beanValidator;

    private AbstractStudyElementValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(CreateStudyModuleValidatorTest.class);

    @BeforeEach
    public void before() {
        AbstractStudyElementValidator validatorInstance = new AbstractStudyElementValidator(new ArrayList<>(), networkService, beanValidator) {};
        validator = spy(validatorInstance);
    }

    @Test
    public void testValidateOrganisationReferences_shouldSuccess() {
        String organizingOrganisationId = "ORG-1";

        Organisation org = DtoInitializer.getOrganisation("ORG-1", "ORG-1");
        Organisation org2 = DtoInitializer.getOrganisation("ORG-2", "ORG-2");

        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference orgRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setOrganisationReferences(Arrays.asList(orgRef, orgRef2));

        validator.validateOrganisationReferences(studyElement.getStudyElementId(), StudyElementType.COURSE_UNIT,
                studyElement.getOrganisationReferences(), organizingOrganisationId);
    }

    @Test
    public void testValidateOrganisationReferences_shouldThrowOrganizingOrganisationMissingValidationExceptionOrgRefsEmpty() {
        String organizingOrganisationId = "ORG-1";

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setOrganisationReferences(null);

        assertThrows(OrganizingOrganisationMissingValidationException.class,
                () -> validator.validateOrganisationReferences(studyElement.getStudyElementId(),
                        StudyElementType.COURSE_UNIT, studyElement.getOrganisationReferences(), organizingOrganisationId));
    }

    @Test
    public void testValidateOrganisationReferences_shouldThrowOrganizingOrganisationMissingValidationExceptionNoMainOrganizer() {
        String organizingOrganisationId = "ORG-1";

        Organisation org = DtoInitializer.getOrganisation("ORG-1", "ORG-1");
        Organisation org2 = DtoInitializer.getOrganisation("ORG-2", "ORG-2");

        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_OTHER_ORGANIZER);
        OrganisationReference orgRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setOrganisationReferences(Arrays.asList(orgRef, orgRef2));

        assertThrows(OrganizingOrganisationMissingValidationException.class,
                () -> validator.validateOrganisationReferences(studyElement.getStudyElementId(),
                        StudyElementType.COURSE_UNIT, studyElement.getOrganisationReferences(), organizingOrganisationId));
    }

    @Test
    public void testValidateOrganisationReferences_shouldMultipleOrganizingOrganisationValidationException() {
        String organizingOrganisationId = "ORG-1";

        Organisation org = DtoInitializer.getOrganisation("ORG-1", "ORG-1");
        Organisation org2 = DtoInitializer.getOrganisation("ORG-2", "ORG-2");
        Organisation org3 = DtoInitializer.getOrganisation("ORG-3", "ORG-3");

        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference orgRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference orgRef3 = DtoInitializer.getOrganisationReference(org3, OrganisationRole.ROLE_OTHER_ORGANIZER);

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setOrganisationReferences(Arrays.asList(orgRef, orgRef2, orgRef3));

        assertThrows(MultipleOrganizingOrganisationValidationException.class,
                () -> validator.validateOrganisationReferences(studyElement.getStudyElementId(),
                        StudyElementType.COURSE_UNIT, studyElement.getOrganisationReferences(), organizingOrganisationId));
    }

    @Test
    public void testValidateOrganisationReferences_shouldOrganizingOrganisationMismatchValidationException() {
        String organizingOrganisationId = "ORG-2";

        Organisation org = DtoInitializer.getOrganisation("ORG-1", "ORG-1");
        Organisation org2 = DtoInitializer.getOrganisation(organizingOrganisationId, "ORG-2");

        OrganisationReference orgRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference orgRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setOrganisationReferences(Arrays.asList(orgRef, orgRef2));

        assertThrows(OrganizingOrganisationMismatchValidationException.class,
                () -> validator.validateOrganisationReferences(studyElement.getStudyElementId(),
                        StudyElementType.COURSE_UNIT, studyElement.getOrganisationReferences(), organizingOrganisationId));
    }

    @Test
    public void testValidateGivenNetworks_shouldSuccess() throws Exception {
        String organizingOrganisationId = "TUNI";

        Validity validity = DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusMonths(1));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organizingOrganisationId);
        networkOrganisation.setValidityInNetwork(validity);
        networkOrganisation.setIsCoordinator(true);

        NetworkOrganisation networkOrganisation2 = new NetworkOrganisation();
        networkOrganisation2.setOrganisationTkCode("JYU");
        networkOrganisation2.setValidityInNetwork(validity);
        networkOrganisation2.setIsCoordinator(false);

        List<NetworkOrganisation> orgs = new ArrayList<>();
        orgs.add(networkOrganisation);
        orgs.add(networkOrganisation2);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", null, orgs,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true);
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowCooperationNetworksMissingValidationException() throws Exception {
        assertThrows(CooperationNetworksMissingValidationException.class,
                () -> validator.validateGivenNetworks(new CourseUnitWriteDTO(), "TUNI", StudyElementType.COURSE_UNIT, true));
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowUnknownCooperationNetworkValidationException() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findValidNetworkById(any())).thenReturn(Optional.empty());

        assertThrows(UnknownCooperationNetworkValidationException.class,
                () -> validator.validateGivenNetworks(studyElement, "TUNI", StudyElementType.COURSE_UNIT, true));
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowNotMemberOfCooperationNetworkValidationException() throws Exception {
        String organizingOrganisationId = "TUNI";

        Validity validity = DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusMonths(1));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode("NOT_TUNI");
        networkOrganisation.setValidityInNetwork(validity);
        networkOrganisation.setIsCoordinator(true);

        NetworkOrganisation networkOrganisation2 = new NetworkOrganisation();
        networkOrganisation2.setOrganisationTkCode("NOT_TUNI2");
        networkOrganisation2.setValidityInNetwork(validity);
        networkOrganisation2.setIsCoordinator(false);

        List<NetworkOrganisation> orgs = new ArrayList<>();
        orgs.add(networkOrganisation);
        orgs.add(networkOrganisation2);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", null, orgs, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertThrows(NotMemberOfCooperationNetworkValidationException.class,
                () -> validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true));
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowNotMemberOfCooperationNetworkValidationExceptionOrgNotValidByValidityTime() throws Exception {
        String organizingOrganisationId = "TUNI";

        Validity validity = DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusMonths(1));

        NetworkOrganisation networkOrganisation = new NetworkOrganisation();
        networkOrganisation.setOrganisationTkCode(organizingOrganisationId);
        networkOrganisation.setValidityInNetwork(DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().plusMonths(1)));
        networkOrganisation.setIsCoordinator(true);

        NetworkOrganisation networkOrganisation2 = new NetworkOrganisation();
        networkOrganisation2.setOrganisationTkCode("NOT_TUNI2");
        networkOrganisation2.setValidityInNetwork(validity);
        networkOrganisation2.setIsCoordinator(false);

        List<NetworkOrganisation> orgs = new ArrayList<>();
        orgs.add(networkOrganisation);
        orgs.add(networkOrganisation2);

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", null, orgs, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");
        studyElement.setCooperationNetworks(Arrays.asList(coopNetwork));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        // Should succeed despite organisation network membership start time being in the future
        assertDoesNotThrow(() -> validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true));

        // Setup with fixed timestamp (membership expired), should still succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusMonths(1)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true));

        // Setup with fixed timestamp (membership in the future), should still succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true));

        // Setup with fixed timestamp (membership valid), should succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusMonths(1)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(studyElement, organizingOrganisationId, StudyElementType.COURSE_UNIT, true));
    }

    @Test
    public void testValidateParentReferences_shouldSuccess() {
        String organizingOrganisationId = "TUNI";
        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForStudyModule("SM-1",  organizingOrganisationId);
        StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForStudyModule("SM-2",  organizingOrganisationId);

        List<StudyElementReference> parents = new ArrayList<>();
        parents.addAll(Arrays.asList(studyElementReference, studyElementReference2));

        doReturn(abstractStudyElementService).when(validator).getServiceForType(any());

        doReturn(Optional.of(new StudyModuleEntity())).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference.getReferenceIdentifier(), organizingOrganisationId);

        doReturn(Optional.of(new StudyModuleEntity())).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference2.getReferenceIdentifier(), organizingOrganisationId);

        validator.validateParentReferences(parents);

        verify(abstractStudyElementService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference.getReferenceIdentifier(), organizingOrganisationId);

        verify(abstractStudyElementService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference2.getReferenceIdentifier(), organizingOrganisationId);
    }
    @Test
    public void testValidateParentReferences_shouldSuccessNoParents() {
        doReturn(abstractStudyElementService).when(validator).getServiceForType(any());
        validator.validateParentReferences(new ArrayList<>());
        verify(abstractStudyElementService, times(0))
                .findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testValidateParentReferences_shouldThrowStudyElementEntityNotFoundException() {
        String organizingOrganisationId = "TUNI";
        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForStudyModule("SM-1", organizingOrganisationId);
        StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForStudyModule("SM-2", organizingOrganisationId);

        List<StudyElementReference> parents = new ArrayList<>();
        parents.addAll(Arrays.asList(studyElementReference, studyElementReference2));

        doReturn(abstractStudyElementService).when(validator).getServiceForType(any());

        doReturn(Optional.of(new StudyModuleEntity())).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference.getReferenceIdentifier(), organizingOrganisationId);

        doReturn(Optional.empty()).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference2.getReferenceIdentifier(), organizingOrganisationId);

        assertThrows(StudyElementEntityNotFoundException.class, () -> validator.validateParentReferences(parents));

        verify(abstractStudyElementService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference.getReferenceIdentifier(), organizingOrganisationId);

        verify(abstractStudyElementService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(
                studyElementReference2.getReferenceIdentifier(), organizingOrganisationId);
    }

    @Test
    public void testValidateNotDuplicate_shouldSuccess() {
        String organizingOrganisationId = "TUNI";

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");

        AbstractStudyElementWriteDTO studyElement2 = new CourseUnitWriteDTO();
        studyElement2.setStudyElementId("SE-2");
        studyElement2.setStudyElementIdentifierCode("SECODE-2");

        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        duplicateTest.add(new KeyHelper(studyElement.getStudyElementId(), organizingOrganisationId));

        doReturn(abstractStudyElementService).when(validator).getServiceForClass(any());

        doReturn(Optional.empty()).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElement.getStudyElementId(), organizingOrganisationId);

        validator.validateNotDuplicate(duplicateTest, studyElement2, organizingOrganisationId);
    }

    @Test
    public void testValidateNotDuplicate_shouldThrowDuplicateEntityValidationException() {
        String organizingOrganisationId = "TUNI";

        AbstractStudyElementWriteDTO studyElement = new CourseUnitWriteDTO();
        studyElement.setStudyElementId("SE-1");
        studyElement.setStudyElementIdentifierCode("SECODE-1");

        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        duplicateTest.add(new KeyHelper(studyElement.getStudyElementId(), organizingOrganisationId));

        doReturn(abstractStudyElementService).when(validator).getServiceForClass(any());

        doReturn(Optional.empty()).when(abstractStudyElementService).findByStudyElementIdAndOrganizingOrganisationId(
                studyElement.getStudyElementId(), organizingOrganisationId);

        assertThrows(DuplicateEntityValidationException.class, () -> validator.validateNotDuplicate(duplicateTest, studyElement, organizingOrganisationId));
    }
}
