package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.DeleteCourseUnitRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class DeleteCourseUnitProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(DeleteCourseUnitProcessorTest.class);

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

    private ObjectMapper objectMapper;
    private DeleteCourseUnitProcessor processor;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = mock(DeleteCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, networkService, organisationService, jmsMessageForwarder,
                objectMapper, messageSchemaService)
            .defaultAnswer(CALLS_REAL_METHODS));
    }


    @Test
    public void testDeleteCourseUnit_ShouldSuccess() throws Exception {
        DeleteCourseUnitRequest request = new DeleteCourseUnitRequest();
        request.setStudyElementId("ID1");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));

        processor.process(exchange);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertEquals("CourseUnit deleted successfully", resp.getMessage());
    }

}
