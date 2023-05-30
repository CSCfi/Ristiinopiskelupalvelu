package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.JsonNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.messageschema.MessageSchemaReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.messageschema.MessageSchemaWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.MessageSchemaEntity;
import fi.uta.ristiinopiskelu.messaging.VersionedMessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageGroup;

import java.util.List;

public interface MessageSchemaService extends Service<MessageSchemaWriteDTO, MessageSchemaEntity, MessageSchemaReadDTO> {

    int getCurrentSchemaVersion();

    String getCurrentSchemaVersionPath(MessageGroup messageGroup, String schemaFile);

    int getPreviousSchemaVersion();

    String getPreviousSchemaVersionPath(MessageGroup messageGroup, String schemaFile);

    VersionedMessageType getMessageTypeForVersion(String messageTypeName, int version);

    <T> T convertObject(Object object, Class<T> targetType);

    <S, T> List<T> convertObjects(List<S> objects, Class<S> sourceType, Class<T> targetType);

    <S, T> JsonNode convertJson(JsonNode jsonNode, Class<S> sourceType, Class<T> targetType);
}
