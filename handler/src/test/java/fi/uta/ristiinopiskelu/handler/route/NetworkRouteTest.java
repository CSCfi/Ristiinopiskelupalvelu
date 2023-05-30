package fi.uta.ristiinopiskelu.handler.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.network.CreateNetworkRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.network.UpdateNetworkRequest;
import fi.uta.ristiinopiskelu.persistence.repository.*;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest
@MockBean(classes = {
    RealisationRepository.class,
    CourseUnitRepository.class,
    DegreeRepository.class,
    NetworkRepository.class,
    RegistrationRepository.class,
    StudiesRepository.class,
    StudyModuleRepository.class,
    OrganisationRepository.class,
    JmsMessageForwarder.class
})
public class NetworkRouteTest {

    @Autowired
    protected CamelContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @EndpointInject("direct:createNetwork")
    private ProducerTemplate createNetworkSource;

    @EndpointInject("mock:createNetwork")
    private MockEndpoint mockCreateNetworkEndpoint;

    @EndpointInject("direct:updateNetwork")
    private ProducerTemplate updateNetworkSource;

    @EndpointInject("mock:updateNetwork")
    private MockEndpoint mockUpdateNetworkSource;

    @EndpointInject("mock:result")
    private MockEndpoint mockCreateResultEndpoint;

    @EndpointInject("mock:result")
    private MockEndpoint mockUpdateResultEndpoint;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private NetworkService networkService;

    @Test
    public void testCreateNetworkRequest_shouldCreateNewNetwork() throws Exception {
        createOrganizations();

        AdviceWith.adviceWith(context, "CREATE_NETWORK_REQUEST", builder -> {
            builder.weaveAddLast().to(mockCreateNetworkEndpoint);
        });

        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String id : Arrays.asList("ORGANISAATIO-1", "ORGANISAATIO-2")) {
            NetworkOrganisation org = new NetworkOrganisation();
            org.setIsCoordinator(false);
            org.setValidityInNetwork(getTestValidity());
            org.setOrganisationTkCode(id);
            networkOrganisations.add(org);
        }

        NetworkWriteDTO network = new NetworkWriteDTO();
        network.setId("TESTIVERKOSTO-ID");
        network.setName(new LocalisedString("uusi", "new", "nya"));
        network.setValidity(getTestValidity());
        network.setPublished(false);
        network.setOrganisations(networkOrganisations);
        network.setNetworkType(NetworkType.FREEDOM_OF_CHOICE);

        NetworkEntity networkEntity = NetworkEntity.fromDto(network);
        when(networkService.create(any())).thenReturn(networkEntity);

        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest();
        createNetworkRequest.setNetwork(network);

        Map<String, Object> headers = getMessageHeaders();
        String body = objectMapper.writeValueAsString(createNetworkRequest);
        createNetworkSource.sendBodyAndHeaders(body, headers);

        List<Exchange> received = mockCreateNetworkEndpoint.getReceivedExchanges();
        String jsonResponse = received.get(0).getIn().getBody(String.class);
        DefaultResponse response = objectMapper.readValue(jsonResponse, DefaultResponse.class);

        mockCreateNetworkEndpoint.expectedMessageCount(1);
        mockCreateNetworkEndpoint.assertIsSatisfied();
        assertTrue(response.getStatus() == Status.OK);
    }
    
    @Test
    public void testUpdateNetworkRequest_shouldUpdateDescriptions() throws  Exception {
        createOrganizations();

        NetworkWriteDTO network = createBaseNetwork();
        AdviceWith.adviceWith(context, "UPDATE_NETWORK_REQUEST", builder -> {
           builder.weaveAddLast().to(mockUpdateResultEndpoint);
        });

        network.setDescription(new LocalisedString("suomi ","englanti", "ruotsi"));

        UpdateNetworkRequest request = new UpdateNetworkRequest();
        request.setNetwork(network);

        Map<String, Object> headers = getMessageHeaders();
        String body = objectMapper.writeValueAsString(request);
        when(networkService.findById(eq(network.getId()))).thenReturn(Optional.of(NetworkEntity.fromDto(network)));
        when(networkService.update((JsonNode) any())).thenReturn(NetworkEntity.fromDto(network));

        updateNetworkSource.sendBodyAndHeaders(body, headers);

        List<Exchange> received = mockUpdateResultEndpoint.getReceivedExchanges();
        String jsonResponse = received.get(0).getIn().getBody(String.class);
        DefaultResponse response = objectMapper.readValue(jsonResponse, DefaultResponse.class);
        mockUpdateResultEndpoint.assertIsSatisfied();
        assertTrue(response.getStatus() == Status.OK);
    }

    private NetworkWriteDTO createBaseNetwork() throws Exception {
        List<NetworkOrganisation> networkOrganisations = new ArrayList<>();
        for(String id : Arrays.asList("ORGANISAATIO-1", "ORGANISAATIO-2")) {
            NetworkOrganisation org = new NetworkOrganisation();
            org.setIsCoordinator(false);
            org.setValidityInNetwork(getTestValidity());
            org.setOrganisationTkCode(id);
            networkOrganisations.add(org);
        }

        NetworkWriteDTO network = new NetworkWriteDTO();
        network.setId("TESTIVERKOSTO-ID");
        network.setName(new LocalisedString("uusi", "new", "nya"));
        network.setValidity(getTestValidity());
        network.setPublished(false);
        network.setNetworkType(NetworkType.FREEDOM_OF_CHOICE);
        network.setOrganisations(networkOrganisations);
        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest();
        createNetworkRequest.setNetwork(network);

        Map<String, Object> headers = getMessageHeaders();
        String body = objectMapper.writeValueAsString(createNetworkRequest);
        createNetworkSource.sendBodyAndHeaders(body, headers);
        return network;
    }

    private Map<String, Object> getMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST.name());
        headers.put(MessageHeader.JMS_XUSERID, "TUNI");
        headers.put(MessageHeader.EPPN, "teppo@tuni.fi");
        return headers;
    }

    private Validity getTestValidity(){
        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now());
        validity.setContinuity(Validity.ContinuityEnum.INDEFINITELY);
        return validity;
    }

    private void createOrganizations() {
        List <OrganisationEntity> organzations = new ArrayList<>();
        for(String id : Arrays.asList("ORGANISAATIO-1", "ORGANISAATIO-2")) {
            OrganisationEntity org = new OrganisationEntity();
            org.setSchemaVersion(6);
            org.setOrganisationName(new LocalisedString(id, id, id));
            org.setQueue(id + "queue");
            org.setAdministratorEmail("testi@testi.fi");
            org.setOrganisationIdentifier(id + "-tk");
            org.setId(id);
            when(organisationService.findById(id)).thenReturn(Optional.of(org));
            organzations.add(org);

        }
        when(organisationService.findByIds(any())).thenReturn(organzations);
    }
}
