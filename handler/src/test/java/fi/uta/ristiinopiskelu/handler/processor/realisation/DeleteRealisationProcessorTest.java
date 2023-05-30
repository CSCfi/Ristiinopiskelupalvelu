package fi.uta.ristiinopiskelu.handler.processor.realisation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.DeleteRealisationRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class DeleteRealisationProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(DeleteRealisationProcessorTest.class);

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    private ObjectMapper objectMapper;
    private DeleteRealisationProcessor processor;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = spy(new DeleteRealisationProcessor(networkService, organisationService, jmsMessageForwarder,
            realisationService, objectMapper, messageSchemaService));
    }

    @Test
    public void testDeleteCourseUnitRealisation_ShouldSuccess() throws Exception {
        DeleteRealisationRequest request = new DeleteRealisationRequest();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");

        processor.process(exchange);

        verify(realisationService, times(1)).delete(any(), any());
    }
}
