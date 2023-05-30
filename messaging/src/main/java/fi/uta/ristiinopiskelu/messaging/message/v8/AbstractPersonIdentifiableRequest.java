package fi.uta.ristiinopiskelu.messaging.message.v8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.PersonIdentifiableMessage;

public abstract class AbstractPersonIdentifiableRequest extends AbstractRequest implements PersonIdentifiableMessage {

    @JsonIgnore
    @Override
    public abstract String getPersonIdentifier();
}
