package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.Message;

public interface PersonIdentifiableMessage extends Message {

    String getPersonIdentifier();
}
