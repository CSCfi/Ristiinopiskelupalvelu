package fi.uta.ristiinopiskelu.messaging.message.current;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractPersonIdentifiableRequest extends AbstractRequest implements PersonIdentifiableMessage {

    @JsonIgnore
    @Override
    public abstract String getPersonIdentifier();
}
