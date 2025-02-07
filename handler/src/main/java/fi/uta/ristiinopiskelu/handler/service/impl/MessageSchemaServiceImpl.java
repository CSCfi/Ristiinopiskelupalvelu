package fi.uta.ristiinopiskelu.handler.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.messageschema.MessageSchemaReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.messageschema.MessageSchemaWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageSchemaFileDoesNotExistException;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageSchemaFileUnreadableException;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.messaging.MessageTypeClassProvider;
import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;
import fi.uta.ristiinopiskelu.messaging.schema.converter.ObjectConverterProvider;
import fi.uta.ristiinopiskelu.persistence.repository.MessageSchemaRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.List;

@Service
public class MessageSchemaServiceImpl extends AbstractService<MessageSchemaWriteDTO, MessageSchemaEntity, MessageSchemaReadDTO> implements MessageSchemaService, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MessageSchemaServiceImpl.class);

    private static final String MESSAGE_SCHEMA_FILE_PATTERN = "/messageschemas/v%s/%s/%s";

    @Autowired
    private MessageSchemaRepository schemaVersionRepository;

    @Autowired
    private ObjectConverterProvider messageSchemaConverterProvider;

    @Autowired
    private MessageTypeClassProvider messageTypeProvider;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${general.message-schema.version.current}")
    private int currentMessageSchemaVersion;

    @Override
    protected MessageSchemaRepository getRepository() {
        return schemaVersionRepository;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected ModelMapper getModelMapper() {
        return modelMapper;
    }


    @Override
    public int getCurrentSchemaVersion() {
        return this.currentMessageSchemaVersion;
    }

    @Override
    public List<Integer> getSupportedSchemaVersions() {
        return messageTypeProvider.getAvailableVersions();
    }

    @Override
    public boolean isSchemaVersionSupported(int version) {
        return messageTypeProvider.hasMessageTypeForVersion(version);
    }

    @Override
    public String getSchemaVersionPath(MessageGroup messageGroup, String schemaFile, String versionPattern) {
        return this.formatSchemaVersionPath(messageGroup, schemaFile, versionPattern);
    }

    @Override
    public String getCurrentSchemaVersionPath(MessageGroup messageGroup, String schemaFile) {
        return this.formatSchemaVersionPath(messageGroup, schemaFile, this.getCurrentSchemaVersion());
    }

    @Override
    public int getPreviousSchemaVersion() {
        return this.getCurrentSchemaVersion() - 1;
    }

    @Override
    public VersionedMessageType getMessageTypeForVersion(String messageTypeName, int version) {
        return this.messageTypeProvider.getMessageTypeForVersion(messageTypeName, version);
    }

    @Override
    public <T> T convertObject(Object object, Class<T> targetType) {
        Assert.notNull(object, "Object cannot be null");
        logger.debug("Converting object from sourceType {} to targetType {}", object.getClass(), targetType);
        return messageSchemaConverterProvider.getConverter(object.getClass(), targetType).convertObject(object);
    }

    @Override
    public <S, T> JsonNode convertJson(JsonNode jsonNode, Class<S> sourceType, Class<T> targetType) {
        logger.debug("Converting JSON from sourceType {} to targetType {}", sourceType, targetType);
        return messageSchemaConverterProvider.getConverter(sourceType, targetType).convertJson(jsonNode);
    }

    protected String formatSchemaVersionPath(MessageGroup messageGroup, String schemaFile, String schemaVersionPattern) {
        Assert.notNull(messageGroup, "MessageGroup cannot be null");
        Assert.hasText(schemaFile, "Schema file cannot be empty");
        Assert.hasText(schemaVersionPattern, "Schema version pattern cannot be empty");

        return String.format(MESSAGE_SCHEMA_FILE_PATTERN, schemaVersionPattern, messageGroup.name().toLowerCase(), schemaFile);
    }

    protected String formatSchemaVersionPath(MessageGroup messageGroup, String schemaFile, int schemaVersion) {
       String path = formatSchemaVersionPath(messageGroup, schemaFile, String.valueOf(schemaVersion));

        try(InputStream schemaFileInputStream = this.getClass().getResourceAsStream(path)) {
            if(schemaFileInputStream == null || schemaFileInputStream.available() == 0) {
                throw new MessageSchemaFileDoesNotExistException("Schema file '" + path + "' does not exist");
            }
        } catch (Exception e) {
            throw new MessageSchemaFileUnreadableException("Error while reading schema file '" + path + "'", e);
        }

        return path;
    }

    @Override
    public void afterPropertiesSet() {
        logger.info("Using JMS message schema version {}", this.currentMessageSchemaVersion);
    }
}
