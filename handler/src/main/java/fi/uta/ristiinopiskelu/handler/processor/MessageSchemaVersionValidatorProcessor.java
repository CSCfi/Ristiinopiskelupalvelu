package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.exception.validation.InvalidMessageSchemaVersionException;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.util.MessageHeaderUtils;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

import java.util.HashMap;

@Component
public class MessageSchemaVersionValidatorProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MessageSchemaVersionValidatorProcessor.class);

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private MessageSchemaService schemaVersionService;

    @Override
    public void process(Exchange exchange) throws Exception {
        MessageType messageType = MessageHeaderUtils.getMessageType(exchange.getIn().getHeaders());
        String organisationId = MessageHeaderUtils.getOrganisationId(exchange.getIn().getHeaders());

        int currentSchemaVersion = schemaVersionService.getCurrentSchemaVersion();
        int previousSchemaVersion = schemaVersionService.getPreviousSchemaVersion();

        // NETWORK messages must always be in the latest version
        if(MessageGroup.NETWORK.contains(messageType.name())) {
            Map<String, Object> headers = new HashMap<>(exchange.getIn().getHeaders());
            headers.put(MessageHeader.SCHEMA_VERSION, schemaVersionService.getCurrentSchemaVersion());
            exchange.getMessage().setHeaders(headers);
            exchange.getMessage().setBody(exchange.getIn().getBody());
            return;
        }

        int schemaVersion;

        try {
            schemaVersion = MessageHeaderUtils.getSchemaVersion(exchange.getIn().getHeaders());
        } catch (Exception e) {
            throw new InvalidMessageSchemaVersionException(e.getMessage());
        }

        OrganisationEntity organisation = organisationService.findById(organisationId)
                .orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, organisationId));

        if(organisation.getSchemaVersion() != schemaVersion) {
            throw new InvalidMessageSchemaVersionException(String.format("Invalid schema version %s supplied, organisation has " +
                    "schema version %s specified in organisation info", schemaVersion, organisation.getSchemaVersion()));
        }

        if(schemaVersion != currentSchemaVersion && schemaVersion != previousSchemaVersion) {
            throw new InvalidMessageSchemaVersionException(String.format("Outdated schema version %s supplied; must be either current (%s) or " +
                    "previous (%s), organisation has schema version %s specified in organisation info", schemaVersion,
                    currentSchemaVersion, previousSchemaVersion, organisation.getSchemaVersion()));
        }
    }
}
