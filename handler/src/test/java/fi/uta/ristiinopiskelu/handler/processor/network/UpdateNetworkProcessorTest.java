package fi.uta.ristiinopiskelu.handler.processor.network;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.network.UpdateNetworkValidator;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.network.UpdateNetworkRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkUpdatedNotification;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class UpdateNetworkProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateNetworkProcessorTest.class);

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private UpdateNetworkValidator validator;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    private ObjectMapper objectMapper;
    private UpdateNetworkProcessor processor;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = spy(new UpdateNetworkProcessor(networkService, organisationService, validator, objectMapper, jmsMessageForwarder));
    }

    @Test
    public void testUpdateNetwork_ShouldSuccess() throws Exception {
        List<String> organisationIds = Arrays.asList("ORG1", "ORG2", "ORG3");
        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String orgId : organisationIds) {
            NetworkOrganisation networkOrg = new NetworkOrganisation();
            networkOrg.setOrganisationTkCode(orgId);
            networkOrganisations.add(networkOrg);
        }

        NetworkWriteDTO network = new NetworkWriteDTO();
        network.setId("TESTI-VERKOSTO2");
        network.setOrganisations(networkOrganisations);
        network.setPublished(true);

        UpdateNetworkRequest request = new UpdateNetworkRequest();
        request.setNetwork(network);

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));

        List<OrganisationEntity> returningOrgEntitys = new ArrayList<>();
        for(String orgId : organisationIds) {
            OrganisationEntity entity = new OrganisationEntity();
            entity.setId(orgId);
            entity.setQueue("org_network_queue-" + orgId);
            entity.setSchemaVersion(1);
            returningOrgEntitys.add(entity);
        }

        when(validator.validateJson(any(), any())).thenReturn(NetworkEntity.fromDto(network));
        when(organisationService.findByIds(any())).thenReturn(returningOrgEntitys);
        when(networkService.update(any(JsonNode.class))).thenReturn(NetworkEntity.fromDto(network));
        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(1);

        processor.process(exchange);

        verify(validator, times(1)).validateJson(any(), any());
        verify(networkService, times(1)).update(any(JsonNode.class));
        verify(organisationService, times(1)).findByIds(any());

        verify(jmsMessageForwarder, times(1)).forwardRequestToOrganisation(anyString(), any(NetworkUpdatedNotification.class), eq(MessageType.NETWORK_UPDATED_NOTIFICATION),
            eq(null), eq(returningOrgEntitys.get(0)));
        verify(jmsMessageForwarder, times(1)).forwardRequestToOrganisation(anyString(), any(NetworkUpdatedNotification.class), eq(MessageType.NETWORK_UPDATED_NOTIFICATION),
            eq(null), eq(returningOrgEntitys.get(1)));
        verify(jmsMessageForwarder, times(1)).forwardRequestToOrganisation(anyString(), any(NetworkUpdatedNotification.class), eq(MessageType.NETWORK_UPDATED_NOTIFICATION),
            eq(null), eq(returningOrgEntitys.get(2)));

        DefaultResponse response = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, response.getStatus());
    }
}
