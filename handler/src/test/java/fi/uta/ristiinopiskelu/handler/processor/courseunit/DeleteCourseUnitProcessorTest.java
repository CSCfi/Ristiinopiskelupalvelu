package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.DeleteCourseUnitRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class DeleteCourseUnitProcessorTest {

    @MockitoBean
    private CourseUnitService courseUnitService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    private JsonMapper jsonMapper;
    private DeleteCourseUnitProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = mock(DeleteCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, networkService, organisationService, jmsMessageForwarder,
                jsonMapper, messageSchemaService)
            .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testDeleteCourseUnit_ShouldSuccess() throws Exception {
        DeleteCourseUnitRequest request = new DeleteCourseUnitRequest();
        request.setStudyElementId("ID1");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));

        processor.process(exchange);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertEquals("CourseUnit deleted successfully", resp.getMessage());
    }

}
