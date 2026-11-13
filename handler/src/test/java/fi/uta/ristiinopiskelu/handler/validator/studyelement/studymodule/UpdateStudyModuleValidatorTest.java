package fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class UpdateStudyModuleValidatorTest {

    @MockitoBean
    private StudyModuleService studyModuleService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private Validator beanValidator;

    private JsonMapper jsonMapper;
    private UpdateStudyModuleValidator validator;

    private final String studyModuleUpdateJsonTemplate =
            """
                {
                    "studyModule": {
                        "studyElementId": "ID1",
                        "studyElementIdentifierCode": "RAIRAI"
                    }
                }
            """;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        validator = spy(new UpdateStudyModuleValidator(new ArrayList<>(), networkService, jsonMapper, beanValidator));
        doReturn(studyModuleService).when(validator).getServiceForClass(any());
    }

    @Test
    public void testValidate_shouldCallAllValidationsOnce() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", jsonMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("organisationReferences", jsonMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("cooperationNetworks", jsonMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

        when(studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.of(new StudyModuleEntity()));
        doNothing().when(validator).validateParentReferences(any());
        doNothing().when(validator).validateOrganisationReferences(any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any());

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(1)).validateParentReferences(any());
        verify(validator, times(1)).validateOrganisationReferences(any(), any());
        verify(validator, times(1)).validateGivenNetworks(any(), any());
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageHeaderException() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(studyModuleUpdateJsonTemplate);
        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, null));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyException() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(null, "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyExceptionNoStudyModule() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(jsonMapper.createObjectNode(), "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowEntityNotFoundException() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", jsonMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("organisationReferences", jsonMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("cooperationNetworks", jsonMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

        when(studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
    }

    @Test
    public void testValidate_shouldCallNoValidation() throws Exception {
        JsonNode updateRequest = jsonMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", null);

        when(studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.of(new StudyModuleEntity()));

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
    }
}
