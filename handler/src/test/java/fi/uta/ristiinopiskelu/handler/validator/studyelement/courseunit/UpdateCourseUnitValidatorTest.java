package fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CompletionOptionWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CompletionOptionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.*;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.Validator;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateCourseUnitValidatorTest {

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private Validator beanValidator;

    private ObjectMapper objectMapper;

    private UpdateCourseUnitValidator validator;

    private static final Logger logger = LoggerFactory.getLogger(UpdateCourseUnitValidatorTest.class);

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        validator = spy(new UpdateCourseUnitValidator(new ArrayList<>(), networkService, realisationService, objectMapper, beanValidator));
        doReturn(courseUnitService).when(validator).getServiceForClass(any());
    }

    @Test
    public void testValidate_shouldCallAllValidationsOnce() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(courseUnitUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("courseUnit")).set("parents", objectMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("courseUnit")).set("organisationReferences", objectMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("courseUnit")).set("cooperationNetworks", objectMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

        when(courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.of(new CourseUnitEntity()));
        doNothing().when(validator).validateParentReferences(any());
        doNothing().when(validator).validateOrganisationReferences(any(), any());
        doNothing().when(validator).validateGivenNetworks(any(), any());
        doNothing().when(validator).validateSubRealisationsHaveAtLeastOneMatchingNetwork(any(), any());

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(1)).validateParentReferences(any());
        verify(validator, times(1)).validateOrganisationReferences(any(), any());
        verify(validator, times(1)).validateGivenNetworks(any(), any());
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageHeaderException() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(courseUnitUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("courseUnit")).set("parents", null);
        assertThrows(InvalidMessageHeaderException.class, () -> validator.validateJson(updateRequest, null));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyException() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(null, "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowInvalidMessageBodyExceptionNoCourseUnit() throws Exception {
        assertThrows(InvalidMessageBodyException.class, () -> validator.validateJson(objectMapper.createObjectNode(), "TUNI"));
    }

    @Test
    public void testValidate_shouldThrowEntityNotFoundException() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(courseUnitUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("courseUnit")).set("parents", objectMapper.convertValue(new ArrayList<StudyElementReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("courseUnit")).set("organisationReferences", objectMapper.convertValue(new ArrayList<OrganisationReference>(), JsonNode.class));
        ((ObjectNode)updateRequest.get("courseUnit")).set("cooperationNetworks", objectMapper.convertValue(new ArrayList<CooperationNetwork>(), JsonNode.class));

        when(courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> validator.validateJson(updateRequest, "TUNI"));

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
        verify(validator, times(0)).validateSubRealisationsHaveAtLeastOneMatchingNetwork(any(), any());
    }

    @Test
    public void testValidate_shouldCallNoValidation() throws Exception {
        JsonNode updateRequest = objectMapper.readTree(courseUnitUpdateJsonTemplate);
        ((ObjectNode)updateRequest.get("courseUnit")).set("parents", null);

        when(courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(any(), any()))
                .thenReturn(Optional.of(new CourseUnitEntity()));

        validator.validateJson(updateRequest, "TUNI");

        verify(validator, times(0)).validateParentReferences(any());
        verify(validator, times(0)).validateOrganisationReferences(any(), any());
        verify(validator, times(0)).validateGivenNetworks(any(), any());
        verify(validator, times(0)).validateSubRealisationsHaveAtLeastOneMatchingNetwork(any(), any());
    }

    @Test
    public void testValidateSubRealisationsHaveAtLeastOneMatchingNetwork_shouldSuccess() throws IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = new CooperationNetwork();
        cooperationNetwork.setId("CN-1");

        CooperationNetwork cooperationNetwork2 = new CooperationNetwork();
        cooperationNetwork2.setId("CN-2");

        CooperationNetwork cooperationNetwork3 = new CooperationNetwork();
        cooperationNetwork3.setId("CN-3");

        CooperationNetwork cooperationNetwork4 = new CooperationNetwork();
        cooperationNetwork4.setId("CN-4");

        CooperationNetwork cooperationNetwork5 = new CooperationNetwork();
        cooperationNetwork5.setId("CN-5");

        List<CooperationNetwork> courseUnitNetworks = new ArrayList<>();
        courseUnitNetworks.add(cooperationNetwork);
        courseUnitNetworks.add(cooperationNetwork2);
        courseUnitNetworks.add(cooperationNetwork3);

        List<CooperationNetwork> oneMatchingNetworkAndOneNotMatching = new ArrayList<>();
        oneMatchingNetworkAndOneNotMatching.add(cooperationNetwork);
        oneMatchingNetworkAndOneNotMatching.add(cooperationNetwork4);

        List<CooperationNetwork> twoMatchingNetworkAndOneNotMatching = new ArrayList<>();
        twoMatchingNetworkAndOneNotMatching.add(cooperationNetwork2);
        twoMatchingNetworkAndOneNotMatching.add(cooperationNetwork3);
        twoMatchingNetworkAndOneNotMatching.add(cooperationNetwork5);

        List<CooperationNetwork> oneMatchingNetwork = new ArrayList<>();
        oneMatchingNetwork.add(cooperationNetwork3);

        RealisationEntity realisation = new RealisationEntity();
        realisation.setCooperationNetworks(oneMatchingNetworkAndOneNotMatching);

        RealisationEntity realisation2 = new RealisationEntity();
        realisation2.setCooperationNetworks(twoMatchingNetworkAndOneNotMatching);

        RealisationEntity realisation3 = new RealisationEntity();
        realisation3.setCooperationNetworks(oneMatchingNetwork);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("ID1", "CODE-1", null,
                courseUnitNetworks, null, null, null);

        when(realisationService.findByStudyElementReference(any(), any())).thenReturn(Arrays.asList(realisation, realisation2, realisation3));

        validator.validateSubRealisationsHaveAtLeastOneMatchingNetwork(courseUnit, organizingOrganisationId);

        verify(realisationService, times(1))
                .findByStudyElementReference(eq(courseUnit.getStudyElementId()), eq(organizingOrganisationId));
    }

    @Test
    public void testValidateSubRealisationsHaveAtLeastOneMatchingNetwork_shouldThrowCooperationNetworksMismatchValidationExceptionNullingCooperationNetworks() throws IOException {
        String organizingOrganisationId = "TUNI";

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("ID1", "CODE-1", null,
                null, null, null, null);

        assertThrows(CooperationNetworksMismatchValidationException.class,
                ()-> validator.validateSubRealisationsHaveAtLeastOneMatchingNetwork(courseUnit, organizingOrganisationId));

        verify(realisationService, times(0)).findByStudyElementReference(any(), any());
    }

    @Test
    public void testValidateSubRealisationsHaveAtLeastOneMatchingNetwork_shouldThrowCooperationNetworksMismatchValidationExceptionRealisationHasNoMatchingNetworks() throws IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = new CooperationNetwork();
        cooperationNetwork.setId("CN-1");

        CooperationNetwork cooperationNetwork2 = new CooperationNetwork();
        cooperationNetwork2.setId("CN-2");

        CooperationNetwork cooperationNetwork3 = new CooperationNetwork();
        cooperationNetwork3.setId("CN-3");

        CooperationNetwork cooperationNetwork4 = new CooperationNetwork();
        cooperationNetwork4.setId("CN-4");

        CooperationNetwork cooperationNetwork5 = new CooperationNetwork();
        cooperationNetwork5.setId("CN-5");

        List<CooperationNetwork> courseUnitNetworks = new ArrayList<>();
        courseUnitNetworks.add(cooperationNetwork);
        courseUnitNetworks.add(cooperationNetwork2);
        courseUnitNetworks.add(cooperationNetwork3);

        List<CooperationNetwork> oneMatchingNetworkAndOneNotMatching = new ArrayList<>();
        oneMatchingNetworkAndOneNotMatching.add(cooperationNetwork);
        oneMatchingNetworkAndOneNotMatching.add(cooperationNetwork4);

        List<CooperationNetwork> noMatchingNetworks = new ArrayList<>();
        noMatchingNetworks.add(cooperationNetwork4);
        noMatchingNetworks.add(cooperationNetwork5);

        RealisationEntity correctRealisation = new RealisationEntity();
        correctRealisation.setRealisationId("R1");
        correctRealisation.setRealisationIdentifierCode("RCODE1");
        correctRealisation.setCooperationNetworks(oneMatchingNetworkAndOneNotMatching);

        RealisationEntity failingRealisation = new RealisationEntity();
        failingRealisation.setRealisationId("R2");
        failingRealisation.setRealisationIdentifierCode("RCODE2");
        failingRealisation.setCooperationNetworks(noMatchingNetworks);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("ID1", "CODE-1", null,
                courseUnitNetworks, null, null, null);

        when(realisationService.findByStudyElementReference(any(), any())).thenReturn(Arrays.asList(correctRealisation, failingRealisation));

        Exception exception = assertThrows(CooperationNetworksMismatchValidationException.class,
                ()-> validator.validateSubRealisationsHaveAtLeastOneMatchingNetwork(courseUnit, organizingOrganisationId));
        assertFalse(exception.getMessage().contains(correctRealisation.getRealisationId()));
        assertTrue(exception.getMessage().contains(failingRealisation.getRealisationId()));

        verify(realisationService, times(1))
                .findByStudyElementReference(eq(courseUnit.getStudyElementId()), eq(organizingOrganisationId));
    }

    @Test
    public void testValidateSubRealisationsHaveAtLeastOneMatchingNetwork_shouldThrowCooperationNetworksMismatchValidationExceptionCourseUnitHasNoMatchingNetworks() throws IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = new CooperationNetwork();
        cooperationNetwork.setId("CN-1");

        CooperationNetwork cooperationNetwork2 = new CooperationNetwork();
        cooperationNetwork2.setId("CN-2");

        CooperationNetwork cooperationNetwork3 = new CooperationNetwork();
        cooperationNetwork3.setId("CN-3");

        CooperationNetwork cooperationNetwork4 = new CooperationNetwork();
        cooperationNetwork4.setId("CN-4");

        CooperationNetwork cooperationNetwork5 = new CooperationNetwork();
        cooperationNetwork5.setId("CN-5");

        List<CooperationNetwork> courseUnitNetworks = new ArrayList<>();
        courseUnitNetworks.add(cooperationNetwork);

        List<CooperationNetwork> noMatchingNetworks = new ArrayList<>();
        noMatchingNetworks.add(cooperationNetwork2);
        noMatchingNetworks.add(cooperationNetwork3);

        List<CooperationNetwork> noMatchingNetworks2 = new ArrayList<>();
        noMatchingNetworks2.add(cooperationNetwork4);
        noMatchingNetworks2.add(cooperationNetwork5);

        RealisationEntity failingRealisation = new RealisationEntity();
        failingRealisation.setRealisationId("R1");
        failingRealisation.setRealisationIdentifierCode("RCODE1");
        failingRealisation.setCooperationNetworks(noMatchingNetworks);

        RealisationEntity failingRealisation2 = new RealisationEntity();
        failingRealisation2.setRealisationId("R2");
        failingRealisation2.setRealisationIdentifierCode("RCODE2");
        failingRealisation2.setCooperationNetworks(noMatchingNetworks);

        RealisationEntity realisationWithBrokenData = new RealisationEntity();
        realisationWithBrokenData.setRealisationId("R3");
        realisationWithBrokenData.setRealisationIdentifierCode("RCODE3");
        realisationWithBrokenData.setCooperationNetworks(null);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("ID1", "CODE-1", null,
                courseUnitNetworks, null, null, null);

        when(realisationService.findByStudyElementReference(any(), any())).thenReturn(Arrays.asList(failingRealisation, failingRealisation2, realisationWithBrokenData));

        Exception exception = assertThrows(CooperationNetworksMismatchValidationException.class,
                ()-> validator.validateSubRealisationsHaveAtLeastOneMatchingNetwork(courseUnit, organizingOrganisationId));

        assertTrue(exception.getMessage().contains(realisationWithBrokenData.getRealisationId()));
        assertTrue(exception.getMessage().contains(failingRealisation.getRealisationId()));
        assertTrue(exception.getMessage().contains(failingRealisation2.getRealisationId()));

        verify(realisationService, times(1))
                .findByStudyElementReference(eq(courseUnit.getStudyElementId()), eq(organizingOrganisationId));
    }

    @Test
    public void testValidateNoAssessmentItemsReferencingRealisationRemoved_noOriginalAssessmentItems_shouldSuccess() {
        CourseUnitEntity originalCourseUnit = EntityInitializer.getCourseUnitEntity("ID1", "TUNI", null, null);
        CourseUnitWriteDTO updCourseUnit = DtoInitializer.getCourseUnit(originalCourseUnit.getStudyElementId(), originalCourseUnit.getStudyElementIdentifierCode(), null,
            null, null, null, null);

        validator.validateNoAssessmentItemsReferencingRealisationRemoved(updCourseUnit, originalCourseUnit);

        verify(realisationService, times(0)).findByAssessmentItemReference(any(), any(), any());
    }

    @Test
    public void testValidateNoAssessmentItemsReferencingRealisationRemoved_noRealisationReferences_shouldSuccess() {
        AssessmentItemEntity assessmentItem = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItem2 = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItem));
        CompletionOptionEntity completionOption2 = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItem2));

        CourseUnitEntity originalCourseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions("ID1", "CODE-1","TUNI",
            null, null, Arrays.asList(completionOption, completionOption2));

        CourseUnitWriteDTO updCourseUnit = DtoInitializer.getCourseUnit(originalCourseUnit.getStudyElementId(), originalCourseUnit.getStudyElementIdentifierCode(), null,
            null, null, null, null);

        when(realisationService.findByAssessmentItemReference(any(), any(), any())).thenReturn(new ArrayList<>());

        validator.validateNoAssessmentItemsReferencingRealisationRemoved(updCourseUnit, originalCourseUnit);

        verify(realisationService, times(2)).findByAssessmentItemReference(any(), any(), any());
    }

    @Test
    public void testValidateNoAssessmentItemsReferencingRealisationRemoved_referencedRealisationsFoundAndUpdatedAssessmentItemsEmpty_shouldThrowRemovedAssessmentItemHasRealisationReferenceValidationException() {
        AssessmentItemEntity assessmentItem = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItem2 = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOption = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItem));
        CompletionOptionEntity completionOption2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(assessmentItem2));

        CourseUnitEntity originalCourseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions("ID1", "CODE-1","TUNI",
            null, null, Arrays.asList(completionOption, completionOption2));

        CourseUnitWriteDTO updCourseUnit = DtoInitializer.getCourseUnit(originalCourseUnit.getStudyElementId(), originalCourseUnit.getStudyElementIdentifierCode(), null,
            null, null, null, null);

        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(assessmentItem.getAssessmentItemId())))
            .thenReturn(null);
        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(assessmentItem2.getAssessmentItemId())))
            .thenReturn(Collections.singletonList(new RealisationEntity()));

        Exception exception = assertThrows(RemovedAssessmentItemHasRealisationReferenceValidationException.class,
            ()-> validator.validateNoAssessmentItemsReferencingRealisationRemoved(updCourseUnit, originalCourseUnit));

        verify(realisationService, times(2)).findByAssessmentItemReference(any(), any(), any());

        assertTrue(exception.getMessage().contains(originalCourseUnit.getStudyElementId()));
    }

    @Test
    public void testValidateNoAssessmentItemsReferencingRealisationRemoved_onlyAddedAndExistingAssessmentItems_shouldSuccess() {
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(assessmentItemEntity2));

        CourseUnitEntity originalCourseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions("ID1", "CODE-1","TUNI",
            null, null, Arrays.asList(completionOptionEntity, completionOptionEntity2));

        AssessmentItemWriteDTO assessmentItem = DtoInitializer.getAssessmentItem("AI-1", null);
        AssessmentItemWriteDTO assessmentItem2 = DtoInitializer.getAssessmentItem("AI-2", null);
        AssessmentItemWriteDTO newAssessmentItem = DtoInitializer.getAssessmentItem("NEWASSESSMENTITEM", null);
        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO-1", null, Collections.singletonList(assessmentItem), null);
        CompletionOptionWriteDTO completionOption2 = DtoInitializer.getCompletionOption("CO-2", null, Arrays.asList(assessmentItem2, newAssessmentItem), null);

        CourseUnitWriteDTO updCourseUnit = DtoInitializer.getCourseUnit(originalCourseUnit.getStudyElementId(), originalCourseUnit.getStudyElementIdentifierCode(), null,
            null, null, null, null);
        updCourseUnit.setCompletionOptions(Arrays.asList(completionOption, completionOption2));

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            originalCourseUnit.getStudyElementId(), originalCourseUnit.getOrganizingOrganisationId(), assessmentItem.getAssessmentItemId());

        StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            originalCourseUnit.getStudyElementId(), originalCourseUnit.getOrganizingOrganisationId(), assessmentItem2.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("ID1", "IDENTIFIERCODE", originalCourseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference), null);

        RealisationEntity realisationEntity2 = EntityInitializer.getRealisationEntity("ID2", "IDENTIFIERCODE2", originalCourseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference2), null);

        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(assessmentItem.getAssessmentItemId())))
            .thenReturn(Collections.singletonList(realisationEntity));

        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(assessmentItem2.getAssessmentItemId())))
            .thenReturn(Collections.singletonList(realisationEntity2));

        validator.validateNoAssessmentItemsReferencingRealisationRemoved(updCourseUnit, originalCourseUnit);

        verify(realisationService, times(2)).findByAssessmentItemReference(any(), any(), any());
    }

    @Test
    public void testValidateNoAssessmentItemsReferencingRealisationRemoved_removedAssessmentItemsWithRealisationReference_shouldSuccess() {
        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(removedAssessmentItemEntity));

        CourseUnitEntity originalCourseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions("ID1", "CODE-1","TUNI",
            null, null, Arrays.asList(completionOptionEntity, completionOptionEntity2));

        AssessmentItemWriteDTO assessmentItem = DtoInitializer.getAssessmentItem(assessmentItemEntity.getAssessmentItemId(), null);
        AssessmentItemWriteDTO newAssessmentItem = DtoInitializer.getAssessmentItem("NEWASSESSMENTITEM", null);
        CompletionOptionWriteDTO completionOption = DtoInitializer.getCompletionOption("CO-1", null, Collections.singletonList(assessmentItem), null);
        CompletionOptionWriteDTO completionOption2 = DtoInitializer.getCompletionOption("CO-2", null,Collections.singletonList(newAssessmentItem), null);

        CourseUnitWriteDTO updCourseUnit = DtoInitializer.getCourseUnit(originalCourseUnit.getStudyElementId(), originalCourseUnit.getStudyElementIdentifierCode(), null,
            null, null, null, null);
        updCourseUnit.setCompletionOptions(Arrays.asList(completionOption, completionOption2));

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            originalCourseUnit.getStudyElementId(), originalCourseUnit.getOrganizingOrganisationId(), assessmentItem.getAssessmentItemId());

        StudyElementReference removedReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            originalCourseUnit.getStudyElementId(), originalCourseUnit.getOrganizingOrganisationId(), removedAssessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("ID1", "IDENTIFIERCODE", originalCourseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference), null);

        RealisationEntity realisationEntityWithRemovedReference = EntityInitializer.getRealisationEntity("ID2", "IDENTIFIERCODE2", originalCourseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(removedReference), null);

        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(assessmentItemEntity.getAssessmentItemId())))
            .thenReturn(Collections.singletonList(realisationEntity));

        when(realisationService.findByAssessmentItemReference(eq(originalCourseUnit.getStudyElementId()), eq(originalCourseUnit.getOrganizingOrganisationId()), eq(removedAssessmentItemEntity.getAssessmentItemId())))
            .thenReturn(Collections.singletonList(realisationEntityWithRemovedReference));

        Exception exception = assertThrows(RemovedAssessmentItemHasRealisationReferenceValidationException.class,
            ()-> validator.validateNoAssessmentItemsReferencingRealisationRemoved(updCourseUnit, originalCourseUnit));

        verify(realisationService, times(2)).findByAssessmentItemReference(any(), any(), any());
        assertTrue(exception.getMessage().contains(removedAssessmentItemEntity.getAssessmentItemId()));
        assertTrue(exception.getMessage().contains(realisationEntityWithRemovedReference.getRealisationId()));
    }

    private final String courseUnitUpdateJsonTemplate =
            "{\n" +
            "    \"courseUnit\": {\n" +
            "        \"studyElementId\": \"ID1\",\n" +
            "        \"studyElementIdentifierCode\": \"RAIRAI\"\n" +
            "    }\n" +
            "}";
}
