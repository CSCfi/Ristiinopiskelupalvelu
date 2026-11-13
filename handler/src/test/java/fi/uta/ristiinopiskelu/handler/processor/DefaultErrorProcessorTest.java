package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.handler.exception.validation.MissingMessageHeaderException;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.exception.validation.CourseUnitAlreadyExistsValidationException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultErrorProcessorTest {

    @Test
    void testCreateErrorResponse_shouldIncludeResolvedErrorCodeInResponseBody_shouldSucceed() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST.name());
        MissingMessageHeaderException exception = new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertEquals(ErrorCode.MISSING_MESSAGE_HEADER.getCode(), response.getErrorCode());
    }

    @Test
    void testCreateErrorResponse_shouldIncludeValidationExceptionMessageInResponseBody_shouldSucceed() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST.name());
        MissingMessageHeaderException exception = new MissingMessageHeaderException(MessageHeader.JMS_XUSERID);

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertTrue(response.getMessage().contains("Message is missing header: JMSXUserID"));
    }

    @Test
    void testCreateErrorResponse_shouldNotIncludeUnknownExceptionMessageInResponseBody_shouldSucceed() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_NETWORK_REQUEST.name());
        RuntimeException exception = new RuntimeException("Database password was invalid");

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertTrue(response.getMessage().contains("Message processing failed due to an internal error."));
        assertFalse(response.getMessage().contains("Database password was invalid"));
    }

    @Test
    void testCreateErrorResponse_shouldUseClientExceptionCodeAndMessageWhenWrapped() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_COURSEUNIT_REQUEST.name());
        CourseUnitAlreadyExistsValidationException cause = new CourseUnitAlreadyExistsValidationException(
            "Course unit already exists [studyElementId: CU-1, organizingOrganisationId: TUNI]");
        CreateFailedException exception = new CreateFailedException(CourseUnitEntity.class, cause);

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertEquals(ErrorCode.COURSE_UNIT_ALREADY_EXISTS.getCode(), response.getErrorCode());
        assertTrue(response.getMessage().contains(cause.getMessage()));
        assertFalse(response.getMessage().contains("Failed to create CourseUnitEntity"));
    }

    @Test
    void testCreateErrorResponse_shouldKeepServiceCodeButHideServiceFailureDetails() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.CREATE_COURSEUNIT_REQUEST.name());
        CreateFailedException exception = new CreateFailedException(
            CourseUnitEntity.class, new RuntimeException("Database password was invalid"));

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertEquals(ErrorCode.SERVICE_CREATE_FAILED.getCode(), response.getErrorCode());
        assertTrue(response.getMessage().contains("Message processing failed due to an internal error."));
        assertFalse(response.getMessage().contains("Database password was invalid"));
    }

    @Test
    void testCreateErrorResponse_shouldIncludeForwardingOutcome() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.MESSAGE_TYPE, MessageType.UPDATE_STUDENT_REQUEST.name());
        MessageForwardingFailedException exception = new MessageForwardingFailedException(
            "Forwarding failed to organisations: ORG-1. Forwarding succeeded to organisations: ORG-2. Message id: MSG-1");

        RistiinopiskeluMessage responseMessage = new DefaultErrorProcessor().createErrorResponse(exchange, exception);
        DefaultResponse response = responseMessage.getBody(DefaultResponse.class);

        assertEquals(ErrorCode.MESSAGE_FORWARDING_FAILED.getCode(), response.getErrorCode());
        assertTrue(response.getMessage().contains(exception.getMessage()));
    }
}
