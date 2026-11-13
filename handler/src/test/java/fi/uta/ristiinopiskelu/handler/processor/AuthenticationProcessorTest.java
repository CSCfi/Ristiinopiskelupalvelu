package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.handler.exception.validation.UnallowedMessageTypeException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
public class AuthenticationProcessorTest {

    private String adminUiUser = "admin-ui-test";
    private String DN = "CN=localhost, OU=gofore, O=TUNI, L=tre, ST=tre, C=fi";

    @Test
    public void testWithoutMessageTypeWithDN_ShouldFail() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, DN);

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);
        MissingMessageHeaderException exception = assertThrows(
            MissingMessageHeaderException.class, () -> processor.process(exchange));
        assertEquals("Message is missing header: messageType", exception.getMessage());
    }

    @Test
    public void testWithoutUserId_ShouldFailWithMissingHeader() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_COURSEUNIT_REQUEST);

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);

        MissingMessageHeaderException exception = assertThrows(
            MissingMessageHeaderException.class, () -> processor.process(exchange));
        assertEquals("Message is missing header: JMSXUserID", exception.getMessage());
    }

    @Test
    public void testUnknownMessageType_ShouldFail() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, DN);
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, "NOT_A_MESSAGE_TYPE");

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);

        assertThrows(UnallowedMessageTypeException.class, () -> processor.process(exchange));
    }

    @Test
    public void testWithNetworkMessageWithDN_ShouldFail() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, DN);
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST);

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);
        assertThrows(UnallowedMessageTypeException.class, () -> processor.process(exchange));

    }

    @Test
    public void testNetworkMessageWithAdminUiUser_ShouldSuccess() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, adminUiUser);
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST);
        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);
        processor.process(exchange);

        String organisationId = (String) exchange.getMessage().getHeader(MessageHeader.JMS_XUSERID);
        assertEquals(adminUiUser, organisationId);
    }
    @Test
    public void testCourseUnitMessageWithAdminUiUser_ShouldFail() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, adminUiUser);
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_COURSEUNIT_REQUEST);

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);
        assertThrows(UnallowedMessageTypeException.class, () ->  processor.process(exchange));

    }
    @Test
    public void testNetworkMessageWithIncorrectUser_ShouldFail() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "notauser");
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST);

        AuthenticationProcessor processor = new AuthenticationProcessor();
        ReflectionTestUtils.setField(processor, "adminUiUser", adminUiUser);
        assertThrows(UnallowedMessageTypeException.class, () ->  processor.process(exchange));
    }
}
