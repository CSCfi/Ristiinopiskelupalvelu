package fi.uta.ristiinopiskelu.handler.validator.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AbstractRealisationValidatorTest {

    private ModelMapper modelMapper;

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private Validator beanValidator;

    private AbstractRealisationValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(CreateRealisationValidatorTest.class);

    @BeforeEach
    public void before() {
        modelMapper = new ModelMapper();
        validator = mock(AbstractRealisationValidator.class, withSettings().useConstructor(modelMapper, realisationService, courseUnitService,
                networkService, beanValidator).defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testValidateStudyElementReferences_shouldSuccess() throws Exception {
        String organizingOrganisationId = "TUNI";
        List<StudyElementReference> references = new ArrayList<>();
        references.add(DtoInitializer.getStudyElementReferenceForCourseUnit("CU-1", organizingOrganisationId));
        references.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-1", organizingOrganisationId, "AI-1"));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setStudyElementReferences(references);

        doReturn(new CourseUnitWriteDTO()).when(validator).getCourseUnitByReference(any());
        doReturn(true).when(validator).hasAtLeastOneMatchingCooperationNetwork(any(), any());

        validator.validateStudyElementReferences(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST);

        verify(validator, times(2)).getCourseUnitByReference(any());
        verify(validator, times(2)).hasAtLeastOneMatchingCooperationNetwork(any(), any());
    }

    @Test
    public void testValidateStudyElementReferences_shouldThrowExceptionNoStudyElementReferences() throws Exception {
        assertThrows(StudyElementReferencesMissingValidationException.class, () ->
                validator.validateStudyElementReferences(new RealisationWriteDTO(), "TUNI", MessageType.CREATE_REALISATION_REQUEST));
    }

    @Test
    public void testValidateStudyElementReferences_shouldThrowOrganizingOrgMismatchException() throws Exception {
        String organizingOrganisationId = "TUNI";

        StudyElementReference validationFailingRef = DtoInitializer.getStudyElementReferenceForCourseUnit("CU-2",  "ANOTHERORGANISATION");

        List<StudyElementReference> references = new ArrayList<>();
        references.add(validationFailingRef);
        references.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-1", organizingOrganisationId, "AI-1"));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setStudyElementReferences(references);

        doReturn(new CourseUnitWriteDTO()).when(validator).getCourseUnitByReference(any());
        doReturn(true).when(validator).hasAtLeastOneMatchingCooperationNetwork(any(), any());

        Exception e = assertThrows(OrganizingOrganisationMismatchValidationException.class,
                () -> validator.validateStudyElementReferences(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));
        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceIdentifier()));
        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceOrganizer()));

        verify(validator, times(1)).getCourseUnitByReference(any());
        verify(validator, times(1)).hasAtLeastOneMatchingCooperationNetwork(any(), any());
    }

    @Test
    public void testValidateStudyElementReferences_shouldThrowReferencedStudyElementMissingValidationException() throws Exception {
        String organizingOrganisationId = "TUNI";

        StudyElementReference validationFailingRef = DtoInitializer.getStudyElementReferenceForCourseUnit("CU-2", organizingOrganisationId);

        List<StudyElementReference> references = new ArrayList<>();
        references.add(validationFailingRef);
        references.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-1", organizingOrganisationId, "AI-1"));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setStudyElementReferences(references);

        doReturn(null).when(validator).getCourseUnitByReference(eq(validationFailingRef));
        doReturn(new CourseUnitWriteDTO()).when(validator).getCourseUnitByReference(eq(references.get(1)));
        doReturn(true).when(validator).hasAtLeastOneMatchingCooperationNetwork(any(), any());

        Exception e = assertThrows(ReferencedStudyElementMissingValidationException.class,
                () -> validator.validateStudyElementReferences(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));
        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceIdentifier()));
        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceOrganizer()));

        verify(validator, times(2)).getCourseUnitByReference(any());
        verify(validator, times(1)).hasAtLeastOneMatchingCooperationNetwork(any(), any());
    }

    @Test
    public void testValidateStudyElementReferences_shouldThrowCooperationNetworksMismatchValidationException() throws Exception {
        String organizingOrganisationId = "TUNI";

        StudyElementReference validationFailingRef = DtoInitializer.getStudyElementReferenceForCourseUnit("CU-2", organizingOrganisationId);

        List<StudyElementReference> references = new ArrayList<>();
        references.add(validationFailingRef);
        references.add(DtoInitializer.getStudyElementReferenceForAssessmentItem("CU-1", organizingOrganisationId, "AI-1"));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setStudyElementReferences(references);

        CourseUnitWriteDTO foundCourseUnit = new CourseUnitWriteDTO();
        CourseUnitWriteDTO validationFailingCourseUnit = new CourseUnitWriteDTO();

        doReturn(validationFailingCourseUnit).when(validator).getCourseUnitByReference(eq(validationFailingRef));
        doReturn(foundCourseUnit).when(validator).getCourseUnitByReference(eq(references.get(1)));
        doReturn(false).when(validator).hasAtLeastOneMatchingCooperationNetwork(eq(validationFailingCourseUnit), any());
        doReturn(true).when(validator).hasAtLeastOneMatchingCooperationNetwork(eq(foundCourseUnit), any());

        Exception e = assertThrows(CooperationNetworksMismatchValidationException.class,
                () -> validator.validateStudyElementReferences(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));

        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceIdentifier()));
        assertTrue(e.getMessage().contains(validationFailingRef.getReferenceOrganizer()));

        verify(validator, times(2)).getCourseUnitByReference(any());
        verify(validator, times(2)).hasAtLeastOneMatchingCooperationNetwork(any(), any());
    }

    @Test
    public void testHasAtLeastOneMatchingCooperationNetwork_shouldSuccess() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Collections.singletonList(coopNetwork));

        assertTrue(validator.hasAtLeastOneMatchingCooperationNetwork(courseUnit, realisation));
    }

    @Test
    public void testHasAtLeastOneMatchingCooperationNetwork_shouldFailRealisationHasNoNetworks() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        RealisationWriteDTO realisation = new RealisationWriteDTO();

        assertFalse(validator.hasAtLeastOneMatchingCooperationNetwork(courseUnit, realisation));
    }

    @Test
    public void testHasAtLeastOneMatchingCooperationNetwork_shouldFailCourseUnitHasNoNetworks() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Collections.singletonList(coopNetwork));

        assertFalse(validator.hasAtLeastOneMatchingCooperationNetwork(courseUnit, realisation));
    }

    @Test
    public void testHasAtLeastOneMatchingCooperationNetwork_shouldFailNoMatchingNetworks() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork3 = DtoInitializer.getCooperationNetwork("CN-3", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));


        CooperationNetwork coopNetwork4 = DtoInitializer.getCooperationNetwork("CN-4", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Arrays.asList(coopNetwork3, coopNetwork4));

        assertFalse(validator.hasAtLeastOneMatchingCooperationNetwork(courseUnit, realisation));
    }

    @Test
    public void testValidateOrganisation_shouldSuccess() throws Exception {
        String organizingOrganisationId = "TUNI";
        Organisation org = DtoInitializer.getOrganisation("TUNI123", organizingOrganisationId);
        Organisation org2 = DtoInitializer.getOrganisation("TUNINSUB1234", "TUNISUB");
        OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference organisationRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setOrganisationReferences(Arrays.asList(organisationRef, organisationRef2));

        validator.validateOrganisation(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST);
    }

    @Test
    public void testValidateOrganisation_shouldThrowOrganizingOrganisationMissingValidationExceptionNoOrganisations() throws Exception {
        assertThrows(OrganizingOrganisationMissingValidationException.class,
                () -> validator.validateOrganisation(new RealisationWriteDTO(), "TUNI", MessageType.CREATE_REALISATION_REQUEST));
    }

    @Test
    public void testValidateOrganisation_shouldThrowOrganizingOrganisationMissingValidationExceptionNoOrganizer() throws Exception {
        String organizingOrganisationId = "TUNI";
        Organisation org = DtoInitializer.getOrganisation("TUNI123", organizingOrganisationId);
        Organisation org2 = DtoInitializer.getOrganisation("TUNINSUB1234", "TUNISUB");
        OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_OTHER_ORGANIZER);
        OrganisationReference organisationRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setOrganisationReferences(Arrays.asList(organisationRef, organisationRef2));

        assertThrows(OrganizingOrganisationMissingValidationException.class,
                () -> validator.validateOrganisation(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));
    }


    @Test
    public void testValidateOrganisation_shouldThrowMultipleOrganizingOrganisationValidationException() throws Exception {
        String organizingOrganisationId = "TUNI";
        Organisation org = DtoInitializer.getOrganisation("TUNI123", organizingOrganisationId);
        Organisation org2 = DtoInitializer.getOrganisation("TUNI456", "TUNI456");
        OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference organisationRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setOrganisationReferences(Arrays.asList(organisationRef, organisationRef2));

        assertThrows(MultipleOrganizingOrganisationValidationException.class,
                () -> validator.validateOrganisation(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));
    }

    @Test
    public void testValidateOrganisation_shouldThrowOrganizingOrganisationMismatchValidationException() throws Exception {
        String organizingOrganisationId = "TUNI";
        Organisation org = DtoInitializer.getOrganisation("TUNI123", "NOT_TUNI");
        Organisation org2 = DtoInitializer.getOrganisation("TUNINSUB1234", "TUNISUB");
        OrganisationReference organisationRef = DtoInitializer.getOrganisationReference(org, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference organisationRef2 = DtoInitializer.getOrganisationReference(org2, OrganisationRole.ROLE_OTHER_ORGANIZER);

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setOrganisationReferences(Arrays.asList(organisationRef, organisationRef2));

        assertThrows(OrganizingOrganisationMismatchValidationException.class,
                () -> validator.validateOrganisation(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST));
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

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true);
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowCooperationNetworksMissingValidationException() throws Exception {
        assertThrows(CooperationNetworksMissingValidationException.class,
                () -> validator.validateGivenNetworks(new RealisationWriteDTO(), "TUNI", MessageType.CREATE_REALISATION_REQUEST, true));
    }

    @Test
    public void testValidateGivenNetworks_shouldThrowUnknownCooperationNetworkValidationException() throws Exception {
        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findValidNetworkById(any())).thenReturn(Optional.empty());

        assertThrows(UnknownCooperationNetworkValidationException.class,
                () -> validator.validateGivenNetworks(realisation, "TUNI", MessageType.CREATE_REALISATION_REQUEST, true));
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

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", null, orgs,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CooperationNetwork coopNetwork2 = DtoInitializer.getCooperationNetwork("CN-2", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Arrays.asList(coopNetwork, coopNetwork2));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertThrows(NotMemberOfCooperationNetworkValidationException.class,
                () -> validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true));
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

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", null, orgs,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusYears(1)), true);

        CooperationNetwork coopNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setCooperationNetworks(Arrays.asList(coopNetwork));

        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        // Should succeed despite organisation network membership start time being in the future
        assertDoesNotThrow(() -> validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true));

        // Setup with fixed timestamp (membership expired), should still succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusMonths(1)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true));

        // Setup with fixed timestamp (membership in the future), should still succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().plusYears(1), OffsetDateTime.now().plusYears(2)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true));

        // Setup with fixed timestamp (membership valid), should succeed
        networkEntity.getOrganisations().get(0).setValidityInNetwork(DtoInitializer.getFixedValidity(OffsetDateTime.now().minusYears(1), OffsetDateTime.now().plusMonths(1)));
        when(networkService.findNetworkById(any())).thenReturn(Optional.of(networkEntity));

        assertDoesNotThrow(() -> validator.validateGivenNetworks(realisation, organizingOrganisationId, MessageType.CREATE_REALISATION_REQUEST, true));
    }

}
