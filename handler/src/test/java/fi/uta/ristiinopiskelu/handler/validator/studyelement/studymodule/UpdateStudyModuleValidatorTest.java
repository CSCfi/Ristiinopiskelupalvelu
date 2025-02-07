package fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateStudyModuleValidatorTest {

    @MockBean
    private StudyModuleService studyModuleService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private Validator beanValidator;

    private ObjectMapper objectMapper;
    private UpdateStudyModuleValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudyModuleValidatorTest.class);

    private final String studyModuleUpdateJsonTemplate =
            "{\n" +
            "    \"studyModule\": {\n" +
            "        \"studyElementId\": \"ID1\",\n" +
            "        \"studyElementIdentifierCode\": \"RAIRAI\"\n" +
            "    }\n" +
            "}";

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        validator = spy(new UpdateStudyModuleValidator(new ArrayList<>(), networkService, objectMapper, beanValidator));
        doReturn(studyModuleService).when(validator).getServiceForClass(any());
    }

    @Test
    public void testValidate_shouldCallAllValidationsOnce() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", objectMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("organisationReferences", objectMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("cooperationNetworks", objectMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

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
        JsonNode updateRequest = objectMapper.readTree(studyModuleUpdateJsonTemplate);
        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, null));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyException() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(null, "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyExceptionNoStudyModule() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(objectMapper.createObjectNode(), "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowEntityNotFoundException() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", objectMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("organisationReferences", objectMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("studyModule")).set("cooperationNetworks", objectMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

        when(studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
    }

    @Test
    public void testValidate_shouldCallNoValidation() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(studyModuleUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("studyModule")).set("parents", null);

        when(studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.of(new StudyModuleEntity()));

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
    }
}
