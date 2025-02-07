package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.validator.realisation.CreateRealisationValidator;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.CreateCourseUnitValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(SpringExtension.class)
public class CreateCourseUnitProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateCourseUnitProcessorTest.class);

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private StudyModuleService studyModuleService;
    
    @MockBean
    private NetworkService networkService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    @MockBean
    private CreateCourseUnitValidator validator;

    @MockBean
    private CreateRealisationValidator realisationValidator;

    private CreateCourseUnitProcessor processor;
    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;

    @BeforeEach
    public void before() {
        modelMapper = new MapperConfig().modelMapper();
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = mock(CreateCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, studyModuleService, networkService, organisationService, jmsMessageForwarder,
                objectMapper, validator, realisationValidator, messageSchemaService)
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
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));

        List<CourseUnitEntity> created = courseUnits
                .stream()
                .map(source -> modelMapper.map(source, CourseUnitEntity.class))
                .collect(Collectors.toList());

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
