package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.Message;

import java.time.OffsetDateTime;

public interface Notification extends Message {

    String getSendingOrganisationTkCode();

    OffsetDateTime getTimestamp();
}
