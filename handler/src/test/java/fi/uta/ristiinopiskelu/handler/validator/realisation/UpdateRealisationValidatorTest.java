package fi.uta.ristiinopiskelu.handler.validator.realisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.Validator;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateRealisationValidatorTest {

    private ObjectMapper objectMapper;

    private ModelMapper modelMapper;

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private Validator beanValidator;

    @Spy
    private UpdateRealisationValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(UpdateRealisationValidatorTest.class);

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        modelMapper = new ModelMapper();
        validator = spy(new UpdateRealisationValidator(modelMapper, objectMapper, realisationService, courseUnitService, networkService, beanValidator));
    }

    @Test
    public void testValidate_shouldNotCallAnySpecificValidations() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailOrganisationIdNull() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));

        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, null));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailOrganisationIdEmpty() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);

        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, ""));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailRealisationJsonObjectMissing() throws Exception {
        JsonNode updateRequest = objectMapper.createObjectNode();

        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowExceptionRealisationNotFound() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);
        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
        verify(validator, times(0)).validateStudyElementReferences(any(), any());
    }

    @Test
    public void testValidate_shouldCallNetworkValidationAndStudyElementRefValidation() throws Exception {
        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN-1", null, true,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);
        ((ObjectNode)updateRequest.get("realisation")).set("cooperationNetworks", objectMapper.convertValue(Collections.singletonList(cooperationNetwork), JsonNode.class));

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));
        doNothing().when(validator).validateStudyElementReferences(any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any());

        validator.validateJson(updateRequest, "TUNI");

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(1)).validateGivenNetworks(any(), any());
        verify(validator, times(1)).validateStudyElementReferences(any(), any());
    }

    @Test
    public void testValidate_shouldCallStudyElementReferenceValidationHasReferences() throws Exception {
        StudyElementReference reference = DtoInitializer.getStudyElementReferenceForCourseUnit("CU-1", "TUNI");

        JsonNode updateRequest = objectMapper.readTree(updateRealisationJson);
        ((ObjectNode)updateRequest.get("realisation")).set("studyElementReferences", objectMapper.convertValue(Collections.singletonList(reference), JsonNode.class));

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));
        doNothing().when(validator).validateStudyElementReferences(any(), any());

        validator.validateJson(updateRequest, "TUNI");

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
        verify(validator, times(1)).validateStudyElementReferences(any(), any());
    }

    private final String updateRealisationJson =
            "{\n" +
            "    \"realisation\": {\n" +
            "        \"realisationId\": \"129177\",\n" +
            "        \"realisationIdentifierCode\": \"129177\",\n" +
            "        \"minSeats\": 20,\n" +
            "        \"maxSeats\": 200,\n" +
            "        \"personReferences\": [\n" +
            "           {\n" +
            "               \"personRole\": {\n" +
            "                   \"key\": \"teacher\",\n" +
            "                   \"codeSetKey\": \"personRole\"\n" +
            "               },\n" +
            "               \"person\": {\n" +
            "                   \"homeEppn\": \"testi2@test.fi\"\n" +
            "               }\n" +
            "           }\n" +
            "        ]\n" +
            "   }\n" +
            "}";
}
