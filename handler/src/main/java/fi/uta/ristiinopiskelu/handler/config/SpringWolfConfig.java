package fi.uta.ristiinopiskelu.handler.config;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import io.github.springwolf.asyncapi.v3.bindings.ChannelBinding;
import io.github.springwolf.asyncapi.v3.bindings.MessageBinding;
import io.github.springwolf.asyncapi.v3.bindings.OperationBinding;
import io.github.springwolf.asyncapi.v3.bindings.jms.JMSChannelBinding;
import io.github.springwolf.asyncapi.v3.bindings.jms.JMSChannelBindingDestinationType;
import io.github.springwolf.asyncapi.v3.bindings.jms.JMSMessageBinding;
import io.github.springwolf.asyncapi.v3.bindings.jms.JMSOperationBinding;
import io.github.springwolf.asyncapi.v3.model.AsyncAPI;
import io.github.springwolf.asyncapi.v3.model.channel.ChannelObject;
import io.github.springwolf.asyncapi.v3.model.channel.ChannelReference;
import io.github.springwolf.asyncapi.v3.model.channel.message.MessageHeaders;
import io.github.springwolf.asyncapi.v3.model.channel.message.MessageObject;
import io.github.springwolf.asyncapi.v3.model.channel.message.MessagePayload;
import io.github.springwolf.asyncapi.v3.model.channel.message.MessageReference;
import io.github.springwolf.asyncapi.v3.model.components.ComponentSchema;
import io.github.springwolf.asyncapi.v3.model.components.Components;
import io.github.springwolf.asyncapi.v3.model.operation.Operation;
import io.github.springwolf.asyncapi.v3.model.operation.OperationAction;
import io.github.springwolf.asyncapi.v3.model.operation.OperationReply;
import io.github.springwolf.asyncapi.v3.model.schema.MultiFormatSchema;
import io.github.springwolf.asyncapi.v3.model.schema.SchemaObject;
import io.github.springwolf.asyncapi.v3.model.schema.SchemaReference;
import io.github.springwolf.core.asyncapi.AsyncApiCustomizer;
import io.github.springwolf.core.asyncapi.schemas.SwaggerSchemaService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Stream;

@Configuration
public class SpringWolfConfig {

    private static final String APPLICATION_JSON = "application/json";
    private static final String HANDLER_CHANNEL = "handler";
    private static final String ORGANISATION_QUEUE_CHANNEL = "<ORGANISATION_QUEUE>";
    private static final String JMS_REPLY_TO_CHANNEL = "JMSReplyTo";
    private static final String JMS_BINDING = "jms";
    private static final String ERROR_CODE_FIELD = "errorCode";

    @Bean
    public AsyncApiCustomizer ripaMessagingDocumentationCustomizer(ObjectProvider<SwaggerSchemaService> schemaServiceProvider) {
        return asyncApi -> {
            SwaggerSchemaService schemaService = schemaServiceProvider.getIfAvailable();
            if (schemaService == null) {
                return;
            }

            asyncApi.setChannels(new LinkedHashMap<>());
            asyncApi.setOperations(new LinkedHashMap<>());
            asyncApi.setComponents(Components.builder()
                .schemas(new LinkedHashMap<>())
                .messages(new LinkedHashMap<>())
                .build());

            addChannels(asyncApi);
            addResponseMessages(asyncApi, schemaService);
            addOperations(asyncApi, schemaService);
            documentErrorCodes(asyncApi);
        };
    }

    private void addChannels(AsyncAPI asyncApi) {
        asyncApi.getChannels().put(HANDLER_CHANNEL, ChannelObject.builder()
            .address(HANDLER_CHANNEL)
            .description("Queue where organisations send requests to RIPA.")
            .messages(new LinkedHashMap<>())
            .bindings(jmsChannelBinding(HANDLER_CHANNEL))
            .build());
        asyncApi.getChannels().put(ORGANISATION_QUEUE_CHANNEL, ChannelObject.builder()
            .address(ORGANISATION_QUEUE_CHANNEL)
            .description("Organisation-specific queue where clients receive forwarded requests and notifications from RIPA.")
            .messages(new LinkedHashMap<>())
            .bindings(jmsChannelBinding(ORGANISATION_QUEUE_CHANNEL))
            .build());
        asyncApi.getChannels().put(JMS_REPLY_TO_CHANNEL, ChannelObject.builder()
            .address(JMS_REPLY_TO_CHANNEL)
            .description("Reply destination from the JMSReplyTo header.")
            .messages(new LinkedHashMap<>())
            .bindings(jmsChannelBinding(JMS_REPLY_TO_CHANNEL))
            .build());
    }

    private void addHandlerHeaders(AsyncAPI asyncApi, SwaggerSchemaService schemaService, MessageType messageType) {
        String schemaName = handlerHeadersSchemaName(schemaService, messageType);
        asyncApi.getComponents().getSchemas().put(schemaName, ComponentSchema.of(SchemaObject.builder()
            .title(schemaName)
            .type(Set.of("object"))
            .required(List.of(
                MessageHeader.MESSAGE_TYPE,
                MessageHeader.SCHEMA_VERSION,
                MessageHeader.EPPN))
            .properties(Map.of(
                MessageHeader.MESSAGE_TYPE, SchemaObject.builder()
                    .type(Set.of("string"))
                    .description("Message type.")
                    .enumValues(List.of(messageType.name()))
                    .build(),
                MessageHeader.SCHEMA_VERSION, SchemaObject.builder()
                    .type(Set.of("integer"))
                    .description("Message schema version.")
                    .build(),
                MessageHeader.EPPN, SchemaObject.builder()
                    .type(Set.of("string"))
                    .description("End-user principal name.")
                    .build()))
            .build()));
    }

    private void addResponseMessages(AsyncAPI asyncApi, SwaggerSchemaService schemaService) {
        addMessage(asyncApi, schemaService, MessageType.DEFAULT_RESPONSE, false);
        addMessage(asyncApi, schemaService, MessageType.JSON_VALIDATION_FAILED_RESPONSE, false);
        addMessage(asyncApi, schemaService, MessageType.REGISTRATION_RESPONSE, false);
        addMessage(asyncApi, schemaService, MessageType.STUDYRECORD_RESPONSE, false);
        addMessage(asyncApi, schemaService, MessageType.STUDENT_RESPONSE, false);

        addResponseChannelMessage(asyncApi, schemaService, MessageType.DEFAULT_RESPONSE);
        addResponseChannelMessage(asyncApi, schemaService, MessageType.JSON_VALIDATION_FAILED_RESPONSE);
        addResponseChannelMessage(asyncApi, schemaService, MessageType.REGISTRATION_RESPONSE);
        addResponseChannelMessage(asyncApi, schemaService, MessageType.STUDYRECORD_RESPONSE);
        addResponseChannelMessage(asyncApi, schemaService, MessageType.STUDENT_RESPONSE);
    }

    private void addOperations(AsyncAPI asyncApi, SwaggerSchemaService schemaService) {
        Arrays.stream(MessageType.values())
            .filter(MessageType::isDocumented)
            .filter(MessageType::isConsumer)
            .forEach(messageType -> addHandlerOperation(asyncApi, schemaService, messageType));

        Arrays.stream(MessageType.values())
            .filter(MessageType::isDocumented)
            .filter(messageType -> !messageType.isConsumer())
            .filter(this::isOrganisationQueueMessage)
            .forEach(messageType -> addOrganisationQueueOperation(asyncApi, schemaService, messageType));
    }

    private boolean isOrganisationQueueMessage(MessageType messageType) {
        return messageType.name().startsWith("FORWARDED_") || messageType.name().endsWith("_NOTIFICATION");
    }

    private void addHandlerOperation(AsyncAPI asyncApi, SwaggerSchemaService schemaService, MessageType messageType) {
        addMessage(asyncApi, schemaService, messageType, true);
        addChannelMessage(asyncApi, HANDLER_CHANNEL, messageType);

        asyncApi.getOperations().put(operationId(messageType, OperationAction.SEND), Operation.builder()
            .operationId(operationId(messageType, OperationAction.SEND))
            .action(OperationAction.SEND)
            .channel(ChannelReference.fromChannel(HANDLER_CHANNEL))
            .title(messageType.name())//title(messageType))
            .description(messageType.name()) //title(messageType))
            .bindings(jmsOperationBinding())
            .messages(List.of(MessageReference.toChannelMessage(HANDLER_CHANNEL, messageType.name())))
            .reply(OperationReply.builder()
                .channel(ChannelReference.fromChannel(JMS_REPLY_TO_CHANNEL))
                .messages(replyMessages(schemaService, messageType))
                .build())
            .build());
    }

    private void addOrganisationQueueOperation(AsyncAPI asyncApi, SwaggerSchemaService schemaService, MessageType messageType) {
        addMessage(asyncApi, schemaService, messageType, false);
        addChannelMessage(asyncApi, ORGANISATION_QUEUE_CHANNEL, messageType);

        asyncApi.getOperations().put(operationId(messageType, OperationAction.RECEIVE), Operation.builder()
            .operationId(operationId(messageType, OperationAction.RECEIVE))
            .action(OperationAction.RECEIVE)
            .channel(ChannelReference.fromChannel(ORGANISATION_QUEUE_CHANNEL))
            .title(messageType.name())
            .description(messageType.name())//title(messageType))
            .bindings(jmsOperationBinding())
            .messages(List.of(MessageReference.toChannelMessage(ORGANISATION_QUEUE_CHANNEL, messageType.name())))
            .build());
    }

    private List<MessageReference> replyMessages(SwaggerSchemaService schemaService, MessageType messageType) {
        return Stream.of(
                successResponse(messageType),
                MessageType.JSON_VALIDATION_FAILED_RESPONSE,
                MessageType.DEFAULT_RESPONSE)
            .map(responseType -> schemaService.getNameFromType(responseType.getClazz()))
            .distinct()
            .map(schemaName -> MessageReference.toChannelMessage(JMS_REPLY_TO_CHANNEL, schemaName))
            .toList();
    }

    private MessageType successResponse(MessageType messageType) {
        return switch (messageType) {
            case CREATE_REGISTRATION_REQUEST -> MessageType.REGISTRATION_RESPONSE;
            case CREATE_STUDYRECORD_REQUEST -> MessageType.STUDYRECORD_RESPONSE;
            case UPDATE_STUDENT_STUDYRIGHT_REQUEST -> MessageType.STUDENT_RESPONSE;
            default -> MessageType.DEFAULT_RESPONSE;
        };
    }

    private void addMessage(AsyncAPI asyncApi, SwaggerSchemaService schemaService, MessageType messageType, boolean handlerHeaders) {
        String schemaName = schemaService.getNameFromType(messageType.getClazz());
        SwaggerSchemaService.ExtractedSchemas schemas = schemaService.resolveSchema(messageType.getClazz(), APPLICATION_JSON);

        if (handlerHeaders) {
            addHandlerHeaders(asyncApi, schemaService, messageType);
        }

        asyncApi.getComponents().getSchemas().put(schemaName, schemas.rootSchema());
        asyncApi.getComponents().getSchemas().putAll(schemas.referencedSchemas());
        asyncApi.getComponents().getMessages().put(messageType.name(), MessageObject.builder()
            .messageId(messageType.name())
            .name(messageType.name())
            .title(messageType.name())
            .contentType(APPLICATION_JSON)
            .headers(handlerHeaders ? MessageHeaders.of(SchemaReference.toSchema(handlerHeadersSchemaName(schemaService, messageType))) : null)
            .payload(MessagePayload.of(MultiFormatSchema.of(SchemaReference.toSchema(schemaName))))
            .bindings(jmsMessageBinding())
            .build());
    }

    private String handlerHeadersSchemaName(SwaggerSchemaService schemaService, MessageType messageType) {
        return schemaService.getSimpleNameFromType(messageType.getClazz()) + "Headers";
    }

    private void documentErrorCodes(AsyncAPI asyncApi) {
        Set<Object> visited = new HashSet<>();
        asyncApi.getComponents().getSchemas().values()
            .forEach(schema -> documentErrorCode(schema, visited));
    }

    private void documentErrorCode(ComponentSchema componentSchema, Set<Object> visited) {
        if (componentSchema == null || componentSchema.getSchema() == null) {
            return;
        }

        documentErrorCode(componentSchema.getSchema(), visited);
    }

    private void documentErrorCode(SchemaObject schema, Set<Object> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }

        if (schema.getProperties() != null) {
            Object errorCodeProperty = schema.getProperties().get(ERROR_CODE_FIELD);
            if (errorCodeProperty != null) {
                documentErrorCodeProperty(errorCodeProperty);
            }

            schema.getProperties().values().forEach(property -> documentNestedSchema(property, visited));
        }

        documentErrorCode(schema.getAdditionalProperties(), visited);
        documentErrorCode(schema.getItems(), visited);
        documentErrorCode(schema.getNot(), visited);
        documentErrorCode(schema.getAllOf(), visited);
        documentErrorCode(schema.getAnyOf(), visited);
        documentErrorCode(schema.getOneOf(), visited);
    }

    private void documentErrorCode(List<ComponentSchema> schemas, Set<Object> visited) {
        if (schemas != null) {
            schemas.forEach(schema -> documentErrorCode(schema, visited));
        }
    }

    private void documentNestedSchema(Object property, Set<Object> visited) {
        if (property instanceof SchemaObject schema) {
            documentErrorCode(schema, visited);
        } else if (property instanceof ComponentSchema componentSchema) {
            documentErrorCode(componentSchema, visited);
        }
    }

    private void documentErrorCodeProperty(Object errorCodeProperty) {
        if (errorCodeProperty instanceof ComponentSchema componentSchema && componentSchema.getSchema() != null) {
            documentErrorCodeProperty(componentSchema.getSchema());
        } else if (errorCodeProperty instanceof SchemaObject errorCodeSchema) {
            errorCodeSchema.setDescription(errorCodeDescription());
            errorCodeSchema.setEnumValues(errorCodes());
        } else if (errorCodeProperty instanceof Map<?, ?> errorCodeSchema) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mutableErrorCodeSchema = (Map<String, Object>) errorCodeSchema;
            mutableErrorCodeSchema.put("description", errorCodeDescription());
            mutableErrorCodeSchema.put("enum", errorCodes());
        }
    }

    private String errorCodeDescription() {
        return "Numerical error code. Set when status is FAILED.\n\n" +
            Arrays.stream(ErrorCode.values())
                .map(errorCode -> "%d = %s".formatted(errorCode.getCode(), errorCode.name()))
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private List<String> errorCodes() {
        return Arrays.stream(ErrorCode.values())
            .map(errorCode -> String.valueOf(errorCode.getCode()))
            .toList();
    }

    private void addChannelMessage(AsyncAPI asyncApi, String channelName, MessageType messageType) {
        asyncApi.getChannels().get(channelName).getMessages().put(
            messageType.name(), MessageReference.toComponentMessage(messageType.name()));
    }

    private void addResponseChannelMessage(AsyncAPI asyncApi, SwaggerSchemaService schemaService, MessageType messageType) {
        asyncApi.getChannels().get(JMS_REPLY_TO_CHANNEL).getMessages().put(
            schemaService.getNameFromType(messageType.getClazz()), MessageReference.toComponentMessage(messageType.name()));
    }

    private Map<String, ChannelBinding> jmsChannelBinding(String destination) {
        return Map.of(JMS_BINDING, JMSChannelBinding.builder()
            .destination(destination)
            .destinationType(JMSChannelBindingDestinationType.QUEUE)
            .build());
    }

    private Map<String, OperationBinding> jmsOperationBinding() {
        return Map.of(JMS_BINDING, JMSOperationBinding.builder().build());
    }

    private Map<String, MessageBinding> jmsMessageBinding() {
        return Map.of(JMS_BINDING, JMSMessageBinding.builder().build());
    }

    private String operationId(MessageType messageType, OperationAction action) {
        return messageType.name().toLowerCase(Locale.ROOT) + "_" + action.name().toLowerCase(Locale.ROOT);
    }
}
