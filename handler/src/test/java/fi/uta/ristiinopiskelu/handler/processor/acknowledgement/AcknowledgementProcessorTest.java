package fi.uta.ristiinopiskelu.handler.processor.acknowledgement;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageBodyException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AcknowledgementProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.validator.acknowledgement.AcknowledgementValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.acknowledgement.Acknowledgement;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AcknowledgementProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(AcknowledgementProcessorTest.class);

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    private AcknowledgementValidator acknowledgementValidator;
    private AcknowledgementProcessor acknowledgementProcessor;
    private ObjectMapper objectMapper;

    private String receivingOrganisationID = "UEF";
    private OrganisationEntity receivingOrganisation;

    @BeforeEach
    public void before () {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
        acknowledgementValidator = new AcknowledgementValidator();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        acknowledgementProcessor = mock(AcknowledgementProcessor.class, withSettings()
            .useConstructor(organisationService, acknowledgementValidator, objectMapper, jmsMessageForwarder)
        .defaultAnswer(CALLS_REAL_METHODS));
        receivingOrganisation = EntityInitializer.getOrganisationEntity(receivingOrganisationID,
            "not-in-use",
            new LocalisedString("foo","bar","baz"), 1);
    }
    @Test
    public void testSendAcknowledgement_ShouldSucceed() throws Exception {

        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        ack.setMessageType(MessageType.CREATE_REGISTRATION_REQUEST);
        ack.setRequestId(UUID.randomUUID().toString());
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(objectMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        acknowledgementProcessor.process(exchange);


        String messageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), messageType);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
    }
    @Test
    public void testSendAcknowledgementWithoutMessageId_ShouldSucceed() throws Exception {

        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        ack.setMessageType(MessageType.CREATE_REGISTRATION_REQUEST);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(objectMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        acknowledgementProcessor.process(exchange);


        String messageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), messageType);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
    }
    @Test
    public void testSendAcknowledgementWithoutRequestId_ShouldSucceed() throws Exception {

        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        ack.setRequestId(UUID.randomUUID().toString());
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(objectMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        acknowledgementProcessor.process(exchange);


        String messageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), messageType);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
    }
        @Test
    public void testSendAcknowledgementWithoutReceiverTKCode_shouldFail() throws Exception {
        Acknowledgement ack = new Acknowledgement();
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(objectMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(InvalidMessageBodyException.class, () -> acknowledgementProcessor.process(exchange));
    }
    @Test
    public void testSendAcknowledgementWithoutSenderHeader_shouldFail () throws Exception {
        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(MissingMessageHeaderException.class, () -> acknowledgementProcessor.process(exchange));

    }
    @Test
    public void testSendAcknowledgementWithEmptyBody_shouldFail () throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody("{}");

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(InvalidMessageBodyException.class, () -> acknowledgementProcessor.process(exchange));

    }
}
