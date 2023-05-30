package fi.uta.ristiinopiskelu.examples.controller;

import fi.uta.ristiinopiskelu.examples.service.ExampleQueueService;
import org.apache.activemq.artemis.jms.client.ActiveMQBytesMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.jms.JMSException;
import javax.jms.Message;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Example controller for handling RIPA message queues through a REST API.
 *
 * POST - /api/queue (sends a JMS message to the RIPA handler queue. expects a JSON payload in body with messageType,
 * eppn and schemaVersion in request headers. see the RIPA messaging docs for the actual message contents)
 *
 * GET - /api/queue (reads JMS messages from your organisations JMS queue for one second. If a listener is used, this is
 * probably useless, see ExampleListener class)
 * */
@RequestMapping("/api/queue")
@RestController
public class ExampleQueueController {

    @Autowired
    private ExampleQueueService queueService;

    /**
     * Sends a JSON message to the handler queue. Needs messageType, eppn and schemaVersion in headers as params, payload JSON in the body
     * @param messageType
     * @param eppn
     * @param json
     * @return
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void send(@RequestHeader("messageType") String messageType, @RequestHeader("eppn") String eppn,
                        @RequestHeader("schemaVersion") String schemaVersion, @RequestBody String json) {
        queueService.send(messageType, eppn, schemaVersion, json);
    }

    /**
     * Reads messages from the organisation queue for one second and returns them. Check the service.
     * @return
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> receive() {
        List<Message> results = queueService.receive();
        return results.stream().map(this::getMessageBody).collect(Collectors.toList());
    }

    /**
     * Reads raw JSON response content from the received message. It could be converted to a proper response object here already too,
     * see the service layer code (queueService#receive()).
     * @param message
     * @return
     */
    private String getMessageBody(Message message) {
        Assert.notNull(message, "Message cannot be null");

        if(!(message instanceof ActiveMQBytesMessage)) {
            throw new IllegalStateException("Received unsupported message: " + message);
        }

        try {
            byte[] messageBody = message.getBody(byte[].class);
            return new String(messageBody, StandardCharsets.UTF_8);
        } catch (JMSException e) {
            throw new IllegalStateException("Error while getting received message body", e);
        }
    }
}
