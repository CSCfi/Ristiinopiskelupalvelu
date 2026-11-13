package fi.uta.ristiinopiskelu.handler.processor.registration;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.CreateRegistrationSelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationReplySelection;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationSelectionItemStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration.RegistrationStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.CreateRegistrationSelectionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationSelectionItemStatusEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.registration.RegistrationStatusEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RegistrationService;
import fi.uta.ristiinopiskelu.handler.validator.registration.RegistrationStatusValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedRegistrationReplyRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.RegistrationReplyRequest;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class RegistrationReplyProcessorTest {

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private RegistrationStatusValidator validator;

    @MockitoBean
    private JmsMessageForwarder messageForwarder;

    private JsonMapper jsonMapper;
    private RegistrationReplyProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = spy(new RegistrationReplyProcessor(registrationService, organisationService, validator,
            jsonMapper, messageForwarder));
    }

    @Test
    public void testRegistrationStatus_ShouldSuccess() throws Exception {
        String sendingOrganisationId = "SENDING-ORG-1";
        String sendingOrganisationQueue = "org-queue-1";
        String receivingOrganisationId = "RECEIVING-ORG-1";

        OrganisationEntity sendingOrganisationEntity = EntityInitializer.getOrganisationEntity(
                sendingOrganisationId, sendingOrganisationQueue, LocalisedStringEntity.from(new LocalisedString("Organisaatio 1", null, null)), 1);

        CreateRegistrationSelection registrationSelection = DtoInitializer.getRegistrationSelectionCourseUnit(
                "OJ1", RegistrationSelectionItemStatus.PENDING);
        CreateRegistrationSelectionEntity registrationSelectionEntity = EntityInitializer.getRegistrationSelectionCourseUnit(
                "OJ1", RegistrationSelectionItemStatusEntity.PENDING);

        RegistrationEntity regEntity = EntityInitializer.getRegistrationEntity(sendingOrganisationId, receivingOrganisationId,
            Collections.singletonList(registrationSelectionEntity), RegistrationStatusEntity.REGISTERED, "CN-1");
        regEntity.setId("REGID-1");

        RegistrationReplyRequest request = new RegistrationReplyRequest();
        request.setRegistrationRequestId(regEntity.getId());
        request.setStatus(RegistrationStatus.REGISTERED);

        registrationSelection.setSelectionItemStatus(RegistrationSelectionItemStatus.ACCEPTED);
        registrationSelection.setSelectionItemStatusInfo("Hyväksytty");
        request.setSelections(Collections.singletonList(RegistrationReplySelection.fromCreate(registrationSelection)));

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));

        when(registrationService.findById(any())).thenReturn(Optional.of(regEntity));
        when(organisationService.findById(any())).thenReturn(Optional.of(sendingOrganisationEntity));

        processor.process(exchange);

        verify(messageForwarder).forwardRequestToOrganisation(anyString(), any(ForwardedRegistrationReplyRequest.class),
            eq(MessageType.FORWARDED_REGISTRATION_REPLY_REQUEST), any(), eq(sendingOrganisationEntity), any());

        String forwardMessageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), forwardMessageType);

        DefaultResponse response = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, response.getStatus());
    }

    @Test
    public void testRegistrationStatus_ShouldFailNoRegistrationEntityFound() throws Exception {
        String sendingOrganisationId = "SENDING-ORG-1";
        String receivingOrganisationId = "RECEIVING-ORG-1";

        String sendingOrganisationQueue = "org-queue-1";

        OrganisationEntity sendingOrganisationEntity = new OrganisationEntity();
        sendingOrganisationEntity.setId(sendingOrganisationId);
        sendingOrganisationEntity.setQueue(sendingOrganisationQueue);

        ExtendedStudent student = new ExtendedStudent();
        student.setHomeEppn("opisk1@eppn.fi");
        student.setFirstNames("Jaakko");

        RegistrationReplyRequest reply = new RegistrationReplyRequest();
        reply.setRegistrationRequestId(UUID.randomUUID().toString());
        reply.setStatus(RegistrationStatus.REGISTERED);
        reply.setHostStudentNumber("1234243");
        reply.setHostStudentEppn("sssdads@csdsa.com");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(reply));

        when(registrationService.findById(any())).thenReturn(Optional.empty());
        
        RegistrationReplyProcessor processor = new RegistrationReplyProcessor(registrationService, organisationService, validator, jsonMapper, messageForwarder);
        assertThrows(EntityNotFoundException.class, () -> processor.process(exchange), "Could not find registration");
    }
}
