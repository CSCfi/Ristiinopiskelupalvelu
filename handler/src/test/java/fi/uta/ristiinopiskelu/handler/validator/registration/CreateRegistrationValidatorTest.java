package fi.uta.ristiinopiskelu.handler.validator.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CreateRegistrationValidatorTest {

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private NetworkService networkService;

    private CreateRegistrationValidator validator;


    @BeforeEach
    public void before() {
        validator = spy(new CreateRegistrationValidator(courseUnitService, realisationService, networkService));
    }

    @Test
    public void testValidate_shouldSuccess() throws Exception {
        String organizingOrganisationId = "TUNI";

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.ACCEPTED);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Collections.singletonList(selection));

        doReturn(Optional.of(new NetworkEntity())).when(networkService).findValidNetworkById(any());
        doReturn(true).when(validator).isOrganisationValidInNetwork(any(), any());
        doNothing().when(validator).verifySelectionsHierarchy(any(), any());
        doNothing().when(validator).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        doNothing().when(validator).verifySelectionsEnrollable(any(), any(), any());

        validator.validateRequest(request, organizingOrganisationId);

        verify(networkService, times(1)).findValidNetworkById(any());
        verify(validator, times(2)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(1)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(1)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(1)).verifySelectionsEnrollable(any(), any(), any());
    }


    @Test
    public void testValidate_shouldThrowMissingMessageHeaderException() throws Exception {
        String organizingOrganisationId = "TUNI";

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.ACCEPTED);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Collections.singletonList(selection));

        assertThrows(MissingMessageHeaderException.class, () -> validator.validateRequest(request, null));

        verify(networkService, times(0)).findValidNetworkById(any());
        verify(validator, times(0)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(0)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(0)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(0)).verifySelectionsEnrollable(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowEntityNotFoundException() throws Exception {
        String organizingOrganisationId = "TUNI";

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.ACCEPTED);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Collections.singletonList(selection));

        doReturn(Optional.empty()).when(networkService).findValidNetworkById(any());

        assertThrows(ValidNetworkNotFoundValidationException.class, () -> validator.validateRequest(request, organizingOrganisationId));

        verify(networkService, times(1)).findValidNetworkById(any());
        verify(validator, times(0)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(0)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(0)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(0)).verifySelectionsEnrollable(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowOrganisationNotValidInNetworkValidationExceptionSendingNotValid() throws Exception {
        String organizingOrganisationId = "TUNI";

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.ACCEPTED);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Collections.singletonList(selection));

        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        doReturn(Optional.of(networkEntity)).when(networkService).findValidNetworkById(any());
        doReturn(false).when(validator).isOrganisationValidInNetwork(eq(networkEntity), eq(request.getSendingOrganisationTkCode()));

        assertThrows(OrganisationNotValidInNetworkValidationException.class, () -> validator.validateRequest(request, organizingOrganisationId));

        verify(networkService, times(1)).findValidNetworkById(any());
        verify(validator, times(1)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(0)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(0)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(0)).verifySelectionsEnrollable(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowOrganisationNotValidInNetworkValidationExceptionReceivingNotValid() throws Exception {
        String organizingOrganisationId = "TUNI";

        RegistrationSelection selection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.ACCEPTED);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Collections.singletonList(selection));

        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        doReturn(Optional.of(networkEntity)).when(networkService).findValidNetworkById(any());
        doReturn(true).when(validator).isOrganisationValidInNetwork(eq(networkEntity), eq(request.getSendingOrganisationTkCode()));
        doReturn(false).when(validator).isOrganisationValidInNetwork(eq(networkEntity), eq(request.getReceivingOrganisationTkCode()));

        assertThrows(OrganisationNotValidInNetworkValidationException.class, () -> validator.validateRequest(request, organizingOrganisationId));

        verify(networkService, times(1)).findValidNetworkById(any());
        verify(validator, times(2)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(0)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(0)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(0)).verifySelectionsEnrollable(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyExceptionMissingSelections() throws Exception {
        String organizingOrganisationId = "TUNI";

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(null);

        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        doReturn(Optional.of(new NetworkEntity())).when(networkService).findValidNetworkById(any());
        doReturn(true).when(validator).isOrganisationValidInNetwork(any(), any());

        assertThrows(InvalidMessageBodyException.class, () -> validator.validateRequest(request, organizingOrganisationId));

        verify(networkService, times(1)).findValidNetworkById(any());
        verify(validator, times(2)).isOrganisationValidInNetwork(any(), any());
        verify(validator, times(0)).verifySelectionsHierarchy(any(), any());
        verify(validator, times(0)).verifyAllSelectionsBelongToGivenNetwork(any(), any(), any());
        verify(validator, times(0)).verifySelectionsEnrollable(any(), any(), any());
    }

    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnTrueTimeRangeValid() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(OffsetDateTime.now().minusYears(1));
        validity.setEnd(OffsetDateTime.now().plusYears(1));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(validity);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertTrue(result);
    }

    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnTrueEndTimeNull() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.INDEFINITELY);
        validity.setStart(OffsetDateTime.now().minusYears(1));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(validity);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertTrue(result);
    }

    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnFalseValidityInFuture() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(OffsetDateTime.now().plusYears(1));
        validity.setEnd(OffsetDateTime.now().plusYears(2));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(validity);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertFalse(result);
    }

    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnFalseValidityInPast() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(OffsetDateTime.now().minusYears(2));
        validity.setEnd(OffsetDateTime.now().minusYears(1));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(validity);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertFalse(result);
    }


    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnFalseValidityMissing() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(null);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertFalse(result);
    }

    @Test
    public void testIsOrganisationValidInNetwork_shouldReturnFalseStartMissing() {
        NetworkEntity networkEntity = new NetworkEntity();
        networkEntity.setId("VERKOSTO-1");

        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(null);
        validity.setEnd(OffsetDateTime.now().plusYears(1));

        NetworkOrganisation organisation = new NetworkOrganisation();
        organisation.setOrganisationTkCode("TUNI");
        organisation.setValidityInNetwork(validity);
        networkEntity.setOrganisations(Collections.singletonList(organisation));

        boolean result = validator.isOrganisationValidInNetwork(networkEntity, organisation.getOrganisationTkCode());
        assertFalse(result);
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldSuccess() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "R1", RegistrationSelectionItemStatus.PENDING, null, null);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Arrays.asList(courseUnitSelection, realisationSelection));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, LocalDate.now().minusYears(1), null);

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(realisationSelection.getSelectionItemId(),
                organizingOrganisationId, null, Collections.singletonList(cooperationNetwork));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(), organizingOrganisationId,
                Collections.singletonList(cooperationNetwork), null);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), organizingOrganisationId);
        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId);

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldSuccessFoundNetworkMissingValidity() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode("JUY");
        request.setReceivingOrganisationTkCode(organizingOrganisationId);
        request.setSelections(Arrays.asList(courseUnitSelection));

        CooperationNetwork cooperationNetworkValidInPast = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, null, null);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(cooperationNetworkValidInPast), null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId);

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldFailNoNetworkFoundWithGivenRegistrationNetworkId() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "R1", RegistrationSelectionItemStatus.PENDING, null, null);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Arrays.asList(courseUnitSelection, realisationSelection));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                "WRONG_NETWORK_ID", null, true, LocalDate.now().minusYears(1), null);

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(realisationSelection.getSelectionItemId(),
                organizingOrganisationId, null, Collections.singletonList(cooperationNetwork));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(cooperationNetwork), null);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), organizingOrganisationId);
        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        assertThrows(RegistrationSelectionNotValidInNetworkValidationException.class,
                () -> validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId));

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldFailCourseUnitFoundNetworkNotEnrollable() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "R1", RegistrationSelectionItemStatus.PENDING, null, null);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode(organizingOrganisationId);
        request.setReceivingOrganisationTkCode("JYU");
        request.setSelections(Arrays.asList(courseUnitSelection, realisationSelection));

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, LocalDate.now().minusYears(1), null);

        CooperationNetwork notEnrollableCooperationNetwork = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, false, LocalDate.now().minusYears(1), null);

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(realisationSelection.getSelectionItemId(),
                organizingOrganisationId, null, Collections.singletonList(cooperationNetwork));
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(notEnrollableCooperationNetwork), null);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), organizingOrganisationId);
        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        assertThrows(RegistrationSelectionNotValidInNetworkValidationException.class,
                () -> validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId));

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldFailFoundNetworkValidInPast() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode("JUY");
        request.setReceivingOrganisationTkCode(organizingOrganisationId);
        request.setSelections(Arrays.asList(courseUnitSelection));

        CooperationNetwork cooperationNetworkValidInPast = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(cooperationNetworkValidInPast), null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        assertThrows(RegistrationSelectionNotValidInNetworkValidationException.class,
                () -> validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldFailFoundNetworkValidInFuture() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode("JUY");
        request.setReceivingOrganisationTkCode(organizingOrganisationId);
        request.setSelections(Arrays.asList(courseUnitSelection));

        CooperationNetwork cooperationNetworkValidInFuture = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, LocalDate.now().plusYears(1), LocalDate.now().plusYears(2));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(cooperationNetworkValidInFuture), null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        assertThrows(RegistrationSelectionNotValidInNetworkValidationException.class,
                () -> validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifyAllSelectionsBelongToGivenNetwork_shouldFailFoundNetworkValidityIncorrect() throws Exception {
        String registrationNetworkId = "NETWORK-1";
        String organizingOrganisationId = "TUNI";

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "CU-1", RegistrationSelectionItemStatus.PENDING);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setEnrolmentDateTime(OffsetDateTime.now());
        request.setSendingOrganisationTkCode("JUY");
        request.setReceivingOrganisationTkCode(organizingOrganisationId);
        request.setSelections(Arrays.asList(courseUnitSelection));

        CooperationNetwork cooperationNetworkIncorrectValidity = DtoInitializer.getCooperationNetwork(
                registrationNetworkId, null, true, null, LocalDate.now().plusYears(2));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitSelection.getSelectionItemId(),
                organizingOrganisationId, Collections.singletonList(cooperationNetworkIncorrectValidity), null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), organizingOrganisationId);

        assertThrows(RegistrationSelectionNotValidInNetworkValidationException.class,
                () -> validator.verifyAllSelectionsBelongToGivenNetwork(registrationNetworkId, request, organizingOrganisationId));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
    }

    @Test
    public void testVerifySelectionsHierarchy_courseUnitSelectionShouldSuccess() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), receivingOrganisationId);

        validator.verifySelectionsHierarchy(Collections.singletonList(courseUnitSelection), receivingOrganisationId);
    }

    @Test
    public void testVerifySelectionsHierarchy_courseUnitSelectionShouldThrowRegistrationSelectionDoesNotExistValidationExceptionSelectionNotFound() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        doReturn(Optional.empty()).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionDoesNotExistValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(courseUnitSelection), receivingOrganisationId));
    }

    @Test
    public void testVerifySelectionsHierarchy_courseUnitSelectionShouldThrowRegistrationSelectionHierarchyValidationExceptionCourseUnitSelectionHasParent() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);
        courseUnitSelection.setParent(new RegistrationSelection());

        doReturn(Optional.empty()).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionDoesNotExistValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(courseUnitSelection), receivingOrganisationId));
    }


    @Test
    public void testVerifySelectionsHierarchy_noCourseUnitSelection_shouldThrowRegistrationSelectionHierarchyValidationException() throws Exception {
        String receivingOrganisationId = "TUNI";
        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "R1", RegistrationSelectionItemStatus.PENDING, null, null);

        assertThrows(RegistrationSelectionHierarchyValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(realisationSelection), receivingOrganisationId));
    }


    @Test
    public void testVerifySelectionsHierarchy_unknownRootSelection_shouldThrowRegistrationSelectionHierarchyValidationException() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem("AI-1", courseUnitSelection);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitEntity.getStudyElementId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionHierarchyValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(assessmentItemSelection), receivingOrganisationId));
    }

    @Test
    public void testVerifySelectionsHierarchy_realisationSelectionWithCourseUnitParent_shouldSuccess() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        StudyElementReference courseUnitReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(courseUnitReference), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitSelection, null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), receivingOrganisationId);

        validator.verifySelectionsHierarchy(Collections.singletonList(realisationSelection), receivingOrganisationId);

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).verifyHierarchyReferences(any(), any(), any());
    }

    @Test
    public void testVerifySelectionsHierarchy_realisationSelectionWithCourseUnitParentRealNotFound_shouldThrowRegistrationSelectionDoesNotExistValidationException() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        StudyElementReference courseUnitReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(courseUnitReference), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitSelection, null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), receivingOrganisationId);

        doReturn(Optional.empty()).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionDoesNotExistValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(realisationSelection), receivingOrganisationId));

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).verifyHierarchyReferences(any(), any(), any());
    }

    @Test
    public void testVerifySelectionsHierarchy_realisationSelectionWithCourseUnitParentRealHasNoRef_shouldThrowRegistrationSelectionReferenceValidationException() throws Exception {
        String receivingOrganisationId = "TUNI";
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        StudyElementReference wrongCourseUnitReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                "CU-2", courseUnitEntity.getOrganizingOrganisationId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(wrongCourseUnitReference), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, courseUnitSelection, null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifySelectionsHierarchy(Collections.singletonList(realisationSelection), receivingOrganisationId));

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).verifyHierarchyReferences(any(), any(), any());
    }

    @Test
    public void testVerifySelectionsHierarchy_realisationSelectionWithAssessmentItemParent_shouldSuccess() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference courseUnitReference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(courseUnitReference), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        doReturn(Optional.of(courseUnitEntity)).when(courseUnitService).findByStudyElementIdAndOrganizingOrganisationId(
                courseUnitSelection.getSelectionItemId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntity)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationSelection.getSelectionItemId(), receivingOrganisationId);

        doNothing().when(validator).verifyHierarchyReferences(any(), any(), any());

        validator.verifySelectionsHierarchy(Collections.singletonList(realisationSelection), receivingOrganisationId);

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(courseUnitService, times(1)).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(1)).verifyHierarchyReferences(any(), any(), any());
    }

    @Test
    public void testVerifyHierarchyReferences_shouldSuccess() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI-2", null);
        AssessmentItemEntity assessmentItemEntity3 = EntityInitializer.getAssessmentItemEntity("AI-3", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Arrays.asList(assessmentItemEntity, assessmentItemEntity2));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(assessmentItemEntity3));
        CompletionOptionEntity completionOptionEntity3 = EntityInitializer.getCompletionOptionEntity("CO-3", null);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Arrays.asList(completionOptionEntity, completionOptionEntity2, completionOptionEntity3));

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity);
    }

    @Test
    public void testVerifyHierarchyReferences_realisationParentNotAssessmentItem_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference courseUnitReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(courseUnitReference), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, completionOptionSelection, null);

        assertThrows(RegistrationSelectionHierarchyValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_realisationHasNoRefToAssessmentItem_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference wrongAssessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), "AI-2");

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(wrongAssessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_realisationHasNoRefToCourseUnit_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference wrongAssessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                "WRONG-ID", courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(wrongAssessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_assessmentItemSelectionHasWrongParent_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection wrongParent = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), wrongParent);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionHierarchyValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_completionOptionSelectionHasWrongParent_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection wrongParent = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), wrongParent);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionHierarchyValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_courseUnitMissingCompletionOptions_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), "AI-1");

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                "CO-1", courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                "AI-1", completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_courseUnitDoesNotHaveSelectedCompletionOption_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Collections.singletonList(completionOptionEntity));

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(),  RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection wrongCompletionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                "CO-2", courseUnitSelection);

        RegistrationSelection assessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
                assessmentItemEntity.getAssessmentItemId(), wrongCompletionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, assessmentItemSelection, null);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifyHierarchyReferences_courseUnitDoesNotHaveSelectedAssessmentItem_shouldFail() throws Exception {
        String receivingOrganisationId = "TUNI";
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", null);
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", null);

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU-1", "CUCODE-1", receivingOrganisationId, null, null);
        courseUnitEntity.setCompletionOptions(Arrays.asList(completionOptionEntity, completionOptionEntity2));

        StudyElementReference assessmentItemRef = DtoInitializer.getStudyElementReferenceForAssessmentItem(
                courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId(), assessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, Collections.singletonList(assessmentItemRef), null);

        RegistrationSelection courseUnitSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        RegistrationSelection completionOptionSelection = DtoInitializer.getRegistrationSelectionCompletionOption(
                completionOptionEntity.getCompletionOptionId(), courseUnitSelection);

        RegistrationSelection wrongAssessmentItemSelection = DtoInitializer.getRegistrationSelectionAssessmentItem(
               "THIS_ID_IS_NOT_IN_COURSE_UNITS_CO", completionOptionSelection);

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntity.getRealisationId(), RegistrationSelectionItemStatus.PENDING, wrongAssessmentItemSelection, null);

        assertThrows(RegistrationSelectionReferenceValidationException.class,
                () -> validator.verifyHierarchyReferences(realisationSelection, courseUnitEntity, realisationEntity));
    }

    @Test
    public void testVerifySelectionsEnrollable_realisationSelectionsAreEnrollable_shouldSuccess() {
        String receivingOrganisationId = "TUNI";

        RealisationEntity realisationEntityEndDateTimeNull = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, null, null);
        realisationEntityEndDateTimeNull.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(30));
        realisationEntityEndDateTimeNull.setEnrollmentEndDateTime(null);

        RealisationEntity realisationEntityEndDateInFuture = EntityInitializer.getRealisationEntity(
                "R2", "RCODE-2", receivingOrganisationId, null, null);
        realisationEntityEndDateInFuture.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(30));
        realisationEntityEndDateInFuture.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(10));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(
                "CU1", "CUCODE-1", receivingOrganisationId, null, null);

        RegistrationSelection realisationSelection1 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityEndDateTimeNull.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        RegistrationSelection realisationSelection2 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityEndDateInFuture.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        RegistrationSelection realisationSelection3 = DtoInitializer.getRegistrationSelectionCourseUnit(
                courseUnitEntity.getStudyElementId(), RegistrationSelectionItemStatus.PENDING);

        doReturn(Optional.of(realisationEntityEndDateTimeNull)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityEndDateTimeNull.getRealisationId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntityEndDateInFuture)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityEndDateInFuture.getRealisationId(), receivingOrganisationId);

        validator.verifySelectionsEnrollable(Arrays.asList(realisationSelection1, realisationSelection2, realisationSelection3), OffsetDateTime.now(), receivingOrganisationId);
    }

    @Test
    public void testVerifySelectionsEnrollable_realisationSelectionsAreEnrollableButNoEnrollmentTimeGiven_shouldFail() {
        String receivingOrganisationId = "TUNI";

        RealisationEntity realisationEntityEndDateTimeNull = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, null, null);
        realisationEntityEndDateTimeNull.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(30));
        realisationEntityEndDateTimeNull.setEnrollmentEndDateTime(null);

        RegistrationSelection realisationSelection1 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityEndDateTimeNull.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        doReturn(Optional.of(realisationEntityEndDateTimeNull)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityEndDateTimeNull.getRealisationId(), receivingOrganisationId);

        assertThrows(RegistrationSelectionEnrollmentTimeValidationException.class,
                () -> validator.verifySelectionsEnrollable(Arrays.asList(realisationSelection1), null, receivingOrganisationId));
    }

    @Test
    public void testVerifySelectionsEnrollable_realisationSelectionsNotEnrollable_shouldFail() {
        String receivingOrganisationId = "TUNI";

        RealisationEntity realisationEntityStartDateNull = EntityInitializer.getRealisationEntity(
                "R1", "RCODE-1", receivingOrganisationId, null, null);
        realisationEntityStartDateNull.setEnrollmentStartDateTime(null);
        realisationEntityStartDateNull.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(30));

        RealisationEntity realisationEntityEnrollmentFuture = EntityInitializer.getRealisationEntity(
                "R2", "RCODE-2", receivingOrganisationId, null, null);
        realisationEntityEnrollmentFuture.setEnrollmentStartDateTime(OffsetDateTime.now().plusDays(1));
        realisationEntityEnrollmentFuture.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(30));

        RealisationEntity realisationEntityEnrollmentPast = EntityInitializer.getRealisationEntity(
                "R3", "RCODE-3", receivingOrganisationId, null, null);
        realisationEntityEnrollmentPast.setEnrollmentStartDateTime(OffsetDateTime.now().minusMonths(1));
        realisationEntityEnrollmentPast.setEnrollmentEndDateTime(OffsetDateTime.now().minusDays(1));

        RegistrationSelection realisationSelection1 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityStartDateNull.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        RegistrationSelection realisationSelection2 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityEnrollmentFuture.getRealisationId(), RegistrationSelectionItemStatus.PENDING, null, null);

        RegistrationSelection realisationSelection3 = DtoInitializer.getRegistrationSelectionRealisation(
                realisationEntityEnrollmentPast.getRealisationId(),  RegistrationSelectionItemStatus.PENDING, null, null);

        doReturn(Optional.of(realisationEntityStartDateNull)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityStartDateNull.getRealisationId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntityEnrollmentFuture)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityEnrollmentFuture.getRealisationId(), receivingOrganisationId);

        doReturn(Optional.of(realisationEntityEnrollmentPast)).when(realisationService).findByIdAndOrganizingOrganisationId(
                realisationEntityEnrollmentPast.getRealisationId(), receivingOrganisationId);

        Exception exception = assertThrows(RegistrationSelectionEnrollmentTimeValidationException.class,
                () -> validator.verifySelectionsEnrollable(Arrays.asList(realisationSelection1, realisationSelection2, realisationSelection3), OffsetDateTime.now(), receivingOrganisationId));

        assertTrue(exception.getMessage().contains(realisationEntityStartDateNull.getRealisationId()));
        assertTrue(exception.getMessage().contains(realisationEntityEnrollmentFuture.getRealisationId()));
        assertTrue(exception.getMessage().contains(realisationEntityEnrollmentPast.getRealisationId()));
    }
}
