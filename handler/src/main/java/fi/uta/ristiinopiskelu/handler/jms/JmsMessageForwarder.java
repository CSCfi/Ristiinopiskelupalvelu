package fi.uta.ristiinopiskelu.handler.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageSchemaVersionException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.Message;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class JmsMessageForwarder {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageForwarder.class);

    private ProducerTemplate producerTemplate;
    private ObjectMapper objectMapper;
    private MessageSchemaService messageSchemaService;

    @Autowired
    public JmsMessageForwarder(ProducerTemplate producerTemplate, ObjectMapper objectMapper, MessageSchemaService messageSchemaService) {
        this.producerTemplate = producerTemplate;
        this.objectMapper = objectMapper;
        this.messageSchemaService = messageSchemaService;
    }

    public void forwardRequestToOrganisation(String entityId, Message message, MessageType messageType, String correlationId,
                                             OrganisationEntity organisation) {
        this.forwardRequestToOrganisation(entityId, message, messageType, correlationId, organisation, null);
    }

    public void forwardRequestToOrganisation(String entityId, Message message, MessageType messageType, String correlationId,
                                             OrganisationEntity organisation, Map<String, String> customHeaders) {
        try {
            forwardRequestToOrganisation(message, messageType, correlationId, organisation, customHeaders);
        } catch(Exception e) {
            throw new MessageForwardingFailedException(messageType + "-message forwarding failed. Message id in index: " + entityId + "." , e);
        }
    }

    public void forwardRequestToOrganisation(Message message, MessageType messageType, String correlationId,
                                             OrganisationEntity organisation) throws Exception {
        this.forwardRequestToOrganisation(message, messageType, correlationId, organisation, null);
    }

    public void forwardRequestToOrganisation(Message message, MessageType messageType, String correlationId,
                                             OrganisationEntity organisation, Map<String, String> customHeaders) throws Exception {

        if(!messageSchemaService.getSupportedSchemaVersions().contains(organisation.getSchemaVersion())) {
            throw new InvalidMessageSchemaVersionException(String.format("Organisation %s has outdated schema version in use; must be one of (%s), " +
                    "organisation has schema version %s specified in organisation info", organisation.getId(),
                StringUtils.collectionToCommaDelimitedString(messageSchemaService.getSupportedSchemaVersions()), organisation.getSchemaVersion()));
        }

        String forwardedMessageType = messageType.name();
        Object forwardedMessage = message;

        if(organisation.getSchemaVersion() != messageSchemaService.getCurrentSchemaVersion()) {
            VersionedMessageType targetMessageType = messageSchemaService.getMessageTypeForVersion(messageType.name(), organisation.getSchemaVersion());
            forwardedMessageType = targetMessageType.name();
            forwardedMessage = messageSchemaService.convertObject(message, targetMessageType.getClazz());
        }

        Map<String, Object> forwardedHeaders = new HashMap<>();
        forwardedHeaders.put(MessageHeader.MESSAGE_TYPE, forwardedMessageType);
        forwardedHeaders.put(MessageHeader.SCHEMA_VERSION, organisation.getSchemaVersion());

        if (correlationId != null) {
            forwardedHeaders.put(MessageHeader.RIPA_CORRELATION_ID, correlationId);
        }

        if(!CollectionUtils.isEmpty(customHeaders)) {
            forwardedHeaders.putAll(customHeaders);
        }

        producerTemplate.sendBodyAndHeaders(
            String.format("jms:queue:%s", organisation.getQueue()),
            objectMapper.writeValueAsString(forwardedMessage), forwardedHeaders);
    }
}
