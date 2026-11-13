package fi.uta.ristiinopiskelu.handler.validator.realisation;

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
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class UpdateRealisationValidatorTest {

    private JsonMapper jsonMapper;

    @MockitoBean
    private CourseUnitService courseUnitService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private RealisationService realisationService;

    @MockitoBean
    private Validator beanValidator;

    @Spy
    private UpdateRealisationValidator validator;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        validator = spy(new UpdateRealisationValidator(jsonMapper, realisationService, courseUnitService, networkService, beanValidator));
    }

    @Test
    public void testValidate_shouldNotCallAnySpecificValidations() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailOrganisationIdNull() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));

        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, null));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailOrganisationIdEmpty() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);

        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, ""));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldFailRealisationJsonObjectMissing() throws Exception {
        JsonNode updateRequest = jsonMapper.createObjectNode();

        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(realisationService, times(0)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any(), any(), eq(false));
        verify(validator, times(0)).validateStudyElementReferences(any(), any(), any());
    }

    @Test
    public void testValidate_shouldThrowExceptionRealisationNotFound() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);
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

        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);
        ((ObjectNode)updateRequest.get("realisation")).set("cooperationNetworks", jsonMapper.convertValue(Collections.singletonList(cooperationNetwork), JsonNode.class));

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

        JsonNode updateRequest = jsonMapper.readTree(updateRealisationJson);
        ((ObjectNode)updateRequest.get("realisation")).set("studyElementReferences", jsonMapper.convertValue(Collections.singletonList(reference), JsonNode.class));

        when(realisationService.findByIdAndOrganizingOrganisationId(any(), any())).thenReturn(Optional.of(new RealisationEntity()));
        doNothing().when(validator).validateStudyElementReferences(any(), any());

        validator.validateJson(updateRequest, "TUNI");

        verify(realisationService, times(1)).findByIdAndOrganizingOrganisationId(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
        verify(validator, times(1)).validateStudyElementReferences(any(), any());
    }

    private final String updateRealisationJson =
            """
                {
                    "realisation": {
                        "realisationId": "129177",
                        "realisationIdentifierCode": "129177",
                        "minSeats": 20,
                        "maxSeats": 200,
                        "personReferences": [
                           {
                               "personRole": {
                                   "key": "teacher",
                                   "codeSetKey": "personRole"
                               },
                               "person": {
                                   "homeEppn": "testi2@test.fi"
                               }
                           }
                        ]
                   }
                }
            """;
}
