package fi.uta.ristiinopiskelu.handler.processor.acknowledgement;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class AcknowledgementProcessorTest {

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    private AcknowledgementValidator acknowledgementValidator;
    private AcknowledgementProcessor acknowledgementProcessor;
    private JsonMapper jsonMapper;

    private String receivingOrganisationID = "UEF";
    private OrganisationEntity receivingOrganisation;

    @BeforeEach
    public void before () {
        jsonMapper = new EsConfig().jsonMapper(null);
        acknowledgementValidator = new AcknowledgementValidator();
        acknowledgementProcessor = mock(AcknowledgementProcessor.class, withSettings()
            .useConstructor(organisationService, acknowledgementValidator, jsonMapper, jmsMessageForwarder)
            .defaultAnswer(CALLS_REAL_METHODS));
        receivingOrganisation = EntityInitializer.getOrganisationEntity(receivingOrganisationID, "not-in-use",
            LocalisedStringEntity.from(new LocalisedString("foo","bar","baz")), 1);
    }
    @Test
    public void testSendAcknowledgement_ShouldSucceed() throws Exception {

        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        ack.setMessageType(MessageType.CREATE_REGISTRATION_REQUEST);
        ack.setRequestId(UUID.randomUUID().toString());
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(jsonMapper.writeValueAsString(ack));

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
        exchange.getIn().setBody(jsonMapper.writeValueAsString(ack));

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
        exchange.getIn().setBody(jsonMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        acknowledgementProcessor.process(exchange);

        String messageType = (String) exchange.getMessage().getHeader(MessageHeader.MESSAGE_TYPE);
        assertEquals(MessageType.DEFAULT_RESPONSE.name(), messageType);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
    }
        @Test
    public void testSendAcknowledgementWithoutReceiverTKCode_shouldFail() {
        Acknowledgement ack = new Acknowledgement();
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(jsonMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(InvalidMessageBodyException.class, () -> acknowledgementProcessor.process(exchange));
    }
    @Test
    public void testSendAcknowledgementWithoutSenderHeader_shouldFail () {
        Acknowledgement ack = new Acknowledgement();
        ack.setReceivingOrganisationTkCode(receivingOrganisationID);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(ack));

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(MissingMessageHeaderException.class, () -> acknowledgementProcessor.process(exchange));

    }
    @Test
    public void testSendAcknowledgementWithEmptyBody_shouldFail () {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody("{}");

        when(organisationService.findById("UEF")).thenReturn(java.util.Optional.ofNullable(receivingOrganisation));
        assertThrows(InvalidMessageBodyException.class, () -> acknowledgementProcessor.process(exchange));

    }
}
