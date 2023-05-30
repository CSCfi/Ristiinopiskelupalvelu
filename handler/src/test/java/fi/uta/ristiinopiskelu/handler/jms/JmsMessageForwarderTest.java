package fi.uta.ristiinopiskelu.handler.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageSchemaVersionException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.ForwardedCreateRegistrationRequest;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class JmsMessageForwarderTest {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageForwarderTest.class);
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private ProducerTemplate producerTemplate;

    @MockBean
    private MessageSchemaService messageSchemaService;

    @BeforeEach
    public void before() {
        jmsMessageForwarder = mock(JmsMessageForwarder.class, withSettings()
            .useConstructor(producerTemplate, new ObjectMapper(), messageSchemaService).defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testForwardRequestToOrganisation_shouldSuccess() throws JsonProcessingException {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", new LocalisedString("Organisaatio", null, null), 7);

        // Initialize message, does not really matter for this test case which message is initialized here
        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

        when(messageSchemaService.getCurrentSchemaVersion()).thenReturn(7);

        jmsMessageForwarder.forwardRequestToOrganisation(forwardedCreateRegistrationRequest.getRegistrationRequestId(), forwardedCreateRegistrationRequest,
                MessageType.FORWARDED_CREATE_REGISTRATION_REQUEST, null, organisationToForwardTo);

        verify(producerTemplate, times(1)).sendBodyAndHeaders(anyString(), any(), any());
        verify(messageSchemaService, times(2)).getCurrentSchemaVersion();
    }

    @Test
    public void testForwardRequestToOrganisation_messageSchemaMissing_shouldThrowMessageForwardingFailedException() throws JsonProcessingException {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", new LocalisedString("Organisaatio", null, null), 7);

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
    public void testForwardRequestToOrganisation_messageForwardingFails_shouldThrowMessageForwardingFailedException() throws JsonProcessingException {
        OrganisationEntity organisationToForwardTo = EntityInitializer.getOrganisationEntity(
            "ORG1", "queue", new LocalisedString("Organisaatio", null, null), 7);

        // Initialize message, does not really matter for this test case which message is initialized here
        ForwardedCreateRegistrationRequest forwardedCreateRegistrationRequest = new ForwardedCreateRegistrationRequest();
        forwardedCreateRegistrationRequest.setRegistrationRequestId("REG-1");

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
