package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.*;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.validator.realisation.CreateRealisationValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.CreateCourseUnitValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class CreateCourseUnitProcessorTest {

    @MockitoBean
    private CourseUnitService courseUnitService;

    @MockitoBean
    private StudyModuleService studyModuleService;
    
    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    @MockitoBean
    private CreateCourseUnitValidator validator;

    @MockitoBean
    private CreateRealisationValidator realisationValidator;

    private CreateCourseUnitProcessor processor;
    private JsonMapper jsonMapper;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = mock(CreateCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, studyModuleService, networkService, organisationService, jmsMessageForwarder,
                jsonMapper, validator, realisationValidator, messageSchemaService)
            .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testCreateCourseUnit_shouldSucceed() throws Exception {
        List<String> courseUnitIds = Arrays.asList("TESTIJAKSO-1", "TESTIJAKSO-2");
        List<CourseUnitWriteDTO> courseUnits = createCourseUnits(courseUnitIds);

        CreateCourseUnitRequest request = new CreateCourseUnitRequest();
        request.setCourseUnits(courseUnits);

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));

        List<CourseUnitEntity> created = courseUnits
                .stream()
                .map(CourseUnitEntity::from)
                .toList();

        List<CompositeIdentifiedEntityModificationResult> result = created.stream()
                .map(cu -> new CompositeIdentifiedEntityModificationResult(ModificationOperationType.CREATE, CompositeIdentifiedEntityType.COURSE_UNIT, null, cu))
                .toList();

        when(courseUnitService.createAll(any(), any(String.class))).thenReturn(result);

        processor.process(exchange);

        verify(courseUnitService, times(1)).createAll(anyList(), eq("TUNI"));
        verify(validator, times(1)).validateObject(anyList(), eq("TUNI"));
        verify(realisationValidator, times(0)).validateCreateCourseUnitRealisations(anyList(), eq("TUNI"), any());

        String messageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), messageType);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertTrue(resp.getMessage().contains("Course unit creation successful"));
    }

    private List<CourseUnitWriteDTO> createCourseUnits(List<String> courseUnitIds) {
        List<CourseUnitWriteDTO> courseUnits = new ArrayList<>();
        for(String id : courseUnitIds) {
            Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
            OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
            CourseUnitWriteDTO cuEntity = DtoInitializer.getCreateCourseUnitRequestDTO(id, "CODE-" + id, new LocalisedString("Opintojakso " + id, null, null),
                    null, Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));
            courseUnits.add(cuEntity);
        }
        return courseUnits;
    }
}
