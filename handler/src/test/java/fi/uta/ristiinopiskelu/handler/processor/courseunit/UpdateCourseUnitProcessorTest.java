package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.UpdateCourseUnitValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateCourseUnitProcessorTest {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateCourseUnitProcessorTest.class);

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    @MockBean
    private UpdateCourseUnitValidator validator;

    private ObjectMapper objectMapper;
    private UpdateCourseUnitProcessor processor;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = mock(UpdateCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, networkService, organisationService, validator,
                jmsMessageForwarder, objectMapper, messageSchemaService)
            .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testUpdateCourseUnit_ShouldSuccess() throws Exception {
        String courseUnitId = "TESTIJAKSO-1";
        CourseUnitEntity cuEntity = EntityInitializer.getCourseUnitEntity("12345", "4CO19KBIOP", "TUNI", null, null);
        cuEntity.setId(courseUnitId);
        cuEntity.setAbbreviation("TJAKS-1");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(
                "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"12345\",\n" +
                "        \"studyElementIdentifierCode\": \"4CO19KBIOP\",\n" +
                "        \"abbreviation\": \"Biop\",\n" +
                "        \"teachingLanguage\": [\n" +
                "            \"fi\"\n" +
                "        ]\n" +
                "    }\n" +
                "}"
        );

        when(courseUnitService.update(any(JsonNode.class), any(String.class))).thenReturn(cuEntity);

        processor.process(exchange);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertEquals("Course unit with id 12345 updated successfully", resp.getMessage());
    }
}
