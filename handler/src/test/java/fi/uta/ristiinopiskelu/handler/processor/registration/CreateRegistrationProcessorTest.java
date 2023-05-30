package fi.uta.ristiinopiskelu.handler.processor.registration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.MessageTemplateInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.validator.registration.CreateRegistrationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CreateRegistrationProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateRegistrationProcessorTest.class);

    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private CreateRegistrationValidator validator;

    @MockBean
    private JmsMessageForwarder messageForwarder;

    private CreateRegistrationProcessor processor;

    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = spy(new CreateRegistrationProcessor(registrationService, organisationService, studentService,
                validator, objectMapper, new ModelMapper(), messageForwarder));
    }

    @Test
    public void testCreateRequestRegistration_ShouldSuccess() throws Exception {
        String sendingOrganisationId = "SENDING-ORG-1";
        String receivingOrganisationId = "RECEIVING-ORG-1";
        String sendingOrganisationQueue = "org-queue-1";

        OrganisationEntity sendingOrganisationEntity = new OrganisationEntity();
        sendingOrganisationEntity.setId(sendingOrganisationId);
        sendingOrganisationEntity.setQueue(sendingOrganisationQueue);

        RegistrationEntity regEntity = new RegistrationEntity();
        regEntity.setId("123456789");
        regEntity.setSendingOrganisationTkCode(sendingOrganisationId);
        regEntity.setReceivingOrganisationTkCode(receivingOrganisationId);

        RegistrationSelection parentCourseUnit = DtoInitializer.getRegistrationSelectionCourseUnitParent("OJ1");

        RegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "TOT1",  RegistrationSelectionItemStatus.PENDING, parentCourseUnit, null);

        CreateRegistrationRequest registrationRequest = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisationId, receivingOrganisationId, "NETWORK-1");
        registrationRequest.setSelections(Collections.singletonList(realisationSelection));

        Validity validity = new Validity();
        validity.setStart(OffsetDateTime.now());
        validity.setContinuity(Validity.ContinuityEnum.INDEFINITELY);

        NetworkOrganisation org1 = new NetworkOrganisation();
        org1.setOrganisationTkCode(sendingOrganisationId);
        org1.setValidityInNetwork(validity);

        NetworkOrganisation org2 = new NetworkOrganisation();
        org2.setOrganisationTkCode(receivingOrganisationId);
        org2.setValidityInNetwork(validity);

        NetworkEntity returnEntity = new NetworkEntity();
        returnEntity.setId("CN-1");
        returnEntity.setOrganisations(Arrays.asList(org1, org2));
        
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(registrationRequest));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, sendingOrganisationId);

        doNothing().when(validator).validateRequest(any(), any());
        when(organisationService.findById(any())).thenReturn(Optional.of(sendingOrganisationEntity));
        when(registrationService.create(any())).thenReturn(regEntity);

        processor.process(exchange);

        verify(registrationService, times(1)).create(any());
        verify(organisationService, times(1)).findById(any());

        String forwardMessageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.REGISTRATION_RESPONSE.name(), forwardMessageType);

        RegistrationResponse response = exchange.getMessage().getBody(RegistrationResponse.class);
        assertEquals(Status.OK, response.getStatus());
        assertTrue(StringUtils.isNotBlank(response.getRegistrationRequestId()));
        assertEquals(regEntity.getId(), response.getRegistrationRequestId());
    }
}
