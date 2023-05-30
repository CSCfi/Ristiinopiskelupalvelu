package fi.uta.ristiinopiskelu.messaging;

import fi.uta.ristiinopiskelu.messaging.message.Message;

public interface VersionedMessageType {

    Class<? extends Message> getClazz();

    String name();
}
