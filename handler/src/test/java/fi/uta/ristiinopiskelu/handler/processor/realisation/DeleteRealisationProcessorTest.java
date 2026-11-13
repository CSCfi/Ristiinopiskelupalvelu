package fi.uta.ristiinopiskelu.handler.processor.realisation;

import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.DeleteRealisationRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class DeleteRealisationProcessorTest {

    @MockitoBean
    private RealisationService realisationService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    private JsonMapper jsonMapper;
    private DeleteRealisationProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = spy(new DeleteRealisationProcessor(networkService, organisationService, jmsMessageForwarder,
            realisationService, jsonMapper, messageSchemaService));
    }

    @Test
    public void testDeleteCourseUnitRealisation_ShouldSuccess() throws Exception {
        DeleteRealisationRequest request = new DeleteRealisationRequest();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");

        processor.process(exchange);

        verify(realisationService, times(1)).delete(any(), any());
    }
}
