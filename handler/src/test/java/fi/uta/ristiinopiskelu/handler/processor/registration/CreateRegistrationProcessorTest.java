package fi.uta.ristiinopiskelu.handler.processor.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelectionParent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.MessageTemplateInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.service.StudentService;
import fi.uta.ristiinopiskelu.handler.service.result.GenericEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.validator.registration.CreateRegistrationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationResponse;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class CreateRegistrationProcessorTest {

    private JsonMapper jsonMapper;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private CreateRegistrationValidator validator;

    @MockitoBean
    private JmsMessageForwarder messageForwarder;

    private CreateRegistrationProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = spy(new CreateRegistrationProcessor(registrationService, organisationService, validator,
            jsonMapper, messageForwarder));
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

        CreateRegistrationSelectionParent parentCourseUnit = DtoInitializer.getRegistrationSelectionCourseUnitParent("OJ1");

        CreateRegistrationSelection realisationSelection = DtoInitializer.getRegistrationSelectionRealisation(
                "TOT1",  RegistrationSelectionItemStatus.PENDING, parentCourseUnit, null);

        CreateRegistrationRequest registrationRequest = MessageTemplateInitializer.getCreateRegistrationRequestTemplate(sendingOrganisationId, receivingOrganisationId, "NETWORK-1");
        registrationRequest.setSelections(Collections.singletonList(realisationSelection));

        ValidityEntity validityEntity = new ValidityEntity();
        validityEntity.setStart(OffsetDateTime.now());
        validityEntity.setContinuity(ValidityEntity.ContinuityEntity.INDEFINITELY);

        NetworkOrganisationEntity orgEntity1 = new NetworkOrganisationEntity();
        orgEntity1.setOrganisationTkCode(sendingOrganisationId);
        orgEntity1.setValidityInNetwork(validityEntity);

        NetworkOrganisationEntity orgEntity2 = new NetworkOrganisationEntity();
        orgEntity2.setOrganisationTkCode(receivingOrganisationId);
        orgEntity2.setValidityInNetwork(validityEntity);

        NetworkEntity returnEntity = new NetworkEntity();
        returnEntity.setId("CN-1");
        returnEntity.setOrganisations(Arrays.asList(orgEntity1, orgEntity2));
        
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(registrationRequest));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, sendingOrganisationId);

        doNothing().when(validator).validateRequest(any(), any());
        when(organisationService.findById(any())).thenReturn(Optional.of(sendingOrganisationEntity));
        when(registrationService.create(any())).thenReturn(List.of(new GenericEntityModificationResult(ModificationOperationType.CREATE, null, regEntity)));
        when(studentService.findByOidOrPersonId(eq(registrationRequest.getPersonOid()), eq(registrationRequest.getPersonId()), any())).thenReturn(Collections.emptyList());
        
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
