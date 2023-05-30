package fi.uta.ristiinopiskelu.messaging.message.current;

import fi.uta.ristiinopiskelu.messaging.message.Message;

import java.time.OffsetDateTime;

public interface Notification extends Message {

    String getSendingOrganisationTkCode();

    OffsetDateTime getTimestamp();
}
