package fi.uta.ristiinopiskelu.handler.jms;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageSchemaVersionException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedCreateRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsConstants;
import org.apache.camel.component.jms.JmsMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class JmsMessageForwarderTest {
    
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private ProducerTemplate producerTemplate;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    @BeforeEach
    public void before() {
        jmsMessageForwarder = mock(JmsMessageForwarder.class, withSettings()
            .useConstructor(producerTemplate, new JsonMapper(), messageSchemaService).defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testForwardRequestToOrganisation_shouldSuccess() {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", LocalisedStringEntity.from(new LocalisedString("Organisaatio", null, null)), 8);

        // Initialize message, does not really matter for this test case which message is initialized here
        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

        when(messageSchemaService.getSupportedSchemaVersions()).thenReturn(List.of(8, 9));
        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(9);
        when(messageSchemaService.getMessageTypeForVersion(eq(MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST.name()), eq(8))).thenReturn(MessageType.FORWARDED_CREATE_STUDYRECORD_REQUEST);

        jmsMessageForwarder.forwardRequestToOrganisation(forwardedCreateRegistrationRequest.getRegistrationRequestId(), forwardedCreateRegistrationRequest,
                MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, null, organisationToForwardTo);

        verify(producerTemplate, times(1)).sendBodyAndHeaders(anyString(), any(), argThat(headers ->
            JmsMessageType.Bytes.equals(headers.get(JmsConstants.JMS_MESSAGE_TYPE))));
        verify(messageSchemaService, times(1)).getCurrentSchemaVersion();
    }

    @Test
    public void testForwardRequestToOrganisation_v9_shouldSendTextMessage() throws Exception {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", LocalisedStringEntity.from(new LocalisedString("Organisaatio", null, null)), 9);

        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

        when(messageSchemaService.getSupportedSchemaVersions()).thenReturn(List.of(8, 9));
        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(9);

        jmsMessageForwarder.forwardRequestToOrganisation(forwardedCreateRegistrationRequest,
            MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, null, organisationToForwardTo,
            Map.of(JmsConstants.JMS_MESSAGE_TYPE, JmsMessageType.Bytes.name()));

        verify(producerTemplate).sendBodyAndHeaders(anyString(), any(), argThat(headers ->
            JmsMessageType.Text.equals(headers.get(JmsConstants.JMS_MESSAGE_TYPE))));
    }

    @Test
    public void testForwardRequestToOrganisation_messageSchemaMissing_shouldThrowMessageForwardingFailedException() {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", LocalisedStringEntity.from(new LocalisedString("Organisaatio", null, null)), 7);

        // Initialize message, does not really matter for this test case which message is initialized here
        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(10);
        when(messageSchemaService.getPreviousSchemaVersion()).thenReturn(6);

        MessageForwardingFailedException e = assertThrows(MessageForwardingFailedException.class,
            () -> jmsMessageForwarder.forwardRequestToOrganisation(forwardedCreateRegistrationRequest.getRegistrationRequestId(), forwardedCreateRegistrationRequest,
                MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, null, organisationToForwardTo));

        assertTrue(e.getMessage().contains(forwardedCreateRegistrationRequest.getRegistrationRequestId()));
        assertTrue(e.getMessage().contains(MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST.toString()));
        assertTrue(e.getCause() instanceof InvalidMessageSchemaVersionException);
    }

    @Test
    public void testForwardRequestToOrganisation_messageForwardingFails_shouldThrowMessageForwardingFailedException() {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", LocalisedStringEntity.from(new LocalisedString("Organisaatio", null, null)), 9);

        // Initialize message, does not really matter for this test case which message is initialized here
        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

        when(messageSchemaService.getSupportedSchemaVersions()).thenReturn(List.of(8, 9));
        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(organisationToForwardTo.getSchemaVersion());

        // producerTemplate.sendBodyAndHeaders will throw exception since procuder template context is not started
        String failureMessage = "FAILED TO SEND FORWARD";
        doThrow(new RuntimeException(failureMessage)).when(producerTemplate).sendBodyAndHeaders(anyString(), any(), any());

        MessageForwardingFailedException e = assertThrows(MessageForwardingFailedException.class,
            () -> jmsMessageForwarder.forwardRequestToOrganisation(forwardedCreateRegistrationRequest.getRegistrationRequestId(), forwardedCreateRegistrationRequest,
            MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, null, organisationToForwardTo));

        assertTrue(e.getMessage().contains(forwardedCreateRegistrationRequest.getRegistrationRequestId()));
        assertTrue(e.getMessage().contains(MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST.toString()));
        assertTrue(e.getCause() instanceof RuntimeException);
        assertEquals(failureMessage, e.getCause().getMessage());
    }

}
