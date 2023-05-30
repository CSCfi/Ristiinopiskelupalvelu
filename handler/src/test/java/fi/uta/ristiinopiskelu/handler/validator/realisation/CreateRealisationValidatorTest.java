package fi.uta.ristiinopiskelu.handler.validator.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.DuplicateEntityValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityAlreadyExistsValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.utils.KeyHelper;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CreateRealisationValidatorTest {

    private ModelMapper modelMapper;

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private Validator beanValidator;

    private CreateRealisationValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(CreateRealisationValidatorTest.class);

    @BeforeEach
    public void before() {
        modelMapper = new ModelMapper();
        validator = spy(new CreateRealisationValidator(modelMapper, realisationService, courseUnitService, networkService, beanValidator));
    }

    @Test
    public void testValidate_shouldThrowExceptionNoRealisations() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateRequest(new CreateRealisationRequest(), "TUNI"));
        verify(validator, times(0)).validateOrganisation(any(), any(), any());
        verify(validator, times(0)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowExceptionNullOrganisationId() throws Exception {
        assertThrows(MissingMessageHeaderException.class, () -> validator.validateRequest(new CreateRealisationRequest(), null));
        verify(validator, times(0)).validateOrganisation(any(), any(), any());
        verify(validator, times(0)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldCallAllValidationsOnce() throws Exception {
        doNothing().when(validator).validateOrganisation(any(), any(), any());
        doNothing().when(validator).validateNotDuplicate(any(), any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any(), any());
        doNothing().when(validator).validateStudyElementReferences(any(), any(), any());

        CreateRealisationRequest request = new CreateRealisationRequest();
        request.setRealisations(Collections.singletonList(new RealisationWriteDTO()));
        validator.validateRequest(request, "TUNI");

        verify(validator, times(1)).validateOrganisation(any(), any(), any());
        verify(validator, times(1)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(1)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(1)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldCallAllValidationsThreeTimes() throws Exception {
        doNothing().when(validator).validateOrganisation(any(), any(), any());
        doNothing().when(validator).validateNotDuplicate(any(), any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any(), any());
        doNothing().when(validator).validateStudyElementReferences(any(), any(), any());

        CreateRealisationRequest request = new CreateRealisationRequest();
        request.setRealisations(Arrays.asList(new RealisationWriteDTO(), new RealisationWriteDTO(), new RealisationWriteDTO()));
        validator.validateRequest(request, "TUNI");

        verify(validator, times(3)).validateOrganisation(any(), any(), any());
        verify(validator, times(3)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(3)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(3)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidateCreateCourseUnitRealisations_shouldThrowExceptionNoOrganisationId() throws Exception {
        assertThrows(MissingMessageHeaderException.class, () -> validator.validateCreateCourseUnitRealisations(null, null, null));

        verify(validator, times(0)).validateOrganisation(any(), any(), any());
        verify(validator, times(0)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
        verify(validator, times(0)).hasAtLeastOneMatchingCooperationNetwork(any(), any());
    }

    @Test
    public void testValidateCreateCourseUnitRealisations_shouldCallAllValidationsThreeTimes() throws Exception {
        doNothing().when(validator).validateOrganisation(any(), any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any(), any());
        doNothing().when(validator).validateHasAtleastOneMatchingCooperationNetwork(any(), any(), any());

        validator.validateCreateCourseUnitRealisations(Arrays.asList(new RealisationWriteDTO(), new RealisationWriteDTO(), new RealisationWriteDTO()), "TUNI", null);

        verify(validator, times(3)).validateOrganisation(any(), any(), any());
        verify(validator, times(0)).validateNotDuplicate(any(), any(), any());
        verify(validator, times(3)).validateGivenNetworks(any(), any(), any());
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
        verify(validator, times(3)).validateHasAtleastOneMatchingCooperationNetwork(any(), any(), any());
    }

    @Test
    public void testValidateNotDuplicate_shouldSuccess() throws Exception {
        String organizingOrganisationId = "TUNI";
        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        duplicateTest.add(new KeyHelper("R2", organizingOrganisationId));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setRealisationId("R1");
        realisation.setRealisationIdentifierCode("RCODE-1");

        when(realisationService.findByIdAndOrganizingOrganisationId(eq(realisation.getRealisationId()), eq(organizingOrganisationId)))
                .thenReturn(Optional.empty());

        validator.validateNotDuplicate(duplicateTest, realisation, organizingOrganisationId);
    }

    @Test
    public void testValidateNotDuplicate_shouldThrowExceptionMessageContainsMultipleRealisations() throws Exception {
        String organizingOrganisationId = "TUNI";
        HashSet<KeyHelper> duplicateTest = new HashSet<>();
        duplicateTest.add(new KeyHelper("R2", organizingOrganisationId));

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setRealisationId("R2");
        realisation.setRealisationIdentifierCode("RCODE-2");

        when(realisationService.findByIdAndOrganizingOrganisationId(eq(realisation.getRealisationId()), eq(organizingOrganisationId)))
                .thenReturn(Optional.empty());

        assertThrows(DuplicateEntityValidationException.class, () -> validator.validateNotDuplicate(duplicateTest, realisation, organizingOrganisationId));
    }

    @Test
    public void testValidateNotDuplicate_shouldThrowExceptionRealisationFoundInElasticsearch() throws Exception {
        String organizingOrganisationId = "TUNI";
        HashSet<KeyHelper> duplicateTest = new HashSet<>();

        RealisationWriteDTO realisation = new RealisationWriteDTO();
        realisation.setRealisationId("R2");
        realisation.setRealisationIdentifierCode("RCODE-2");

        when(realisationService.findByIdAndOrganizingOrganisationId(eq(realisation.getRealisationId()), eq(organizingOrganisationId)))
                .thenReturn(Optional.of(new RealisationEntity()));

        assertThrows(EntityAlreadyExistsValidationException.class, () -> validator.validateNotDuplicate(duplicateTest, realisation, organizingOrganisationId));
    }
}
