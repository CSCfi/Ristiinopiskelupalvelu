package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;

public interface PersonIdentifiableMessage extends Message {

    String getPersonIdentifier();
}
