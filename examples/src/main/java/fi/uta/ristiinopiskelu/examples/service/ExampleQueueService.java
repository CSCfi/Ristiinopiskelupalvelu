package fi.uta.ristiinopiskelu.examples.service;

import jakarta.jms.Message;
import java.util.List;

public interface ExampleQueueService {

    void send(String messageType, String eppn, String schemaVersion, String json);

    List<Message> receive();
}
