package fi.uta.ristiinopiskelu.examples.service;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import java.util.ArrayList;
import java.util.List;

/**
 * An example service that reads and sends JSON messages to and from the JMS queue.
 */
@Component
public class ExampleQueueServiceImpl implements ExampleQueueService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleQueueServiceImpl.class);

    @Value("${general.organisation-queue}")
    private String organisationQueue;

    @Value("${general.handler-queue}")
    private String handlerQueue;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Transactional
    @Override
    public void send(String messageType, String eppn, String schemaVersion, String json) {
        Assert.hasText(messageType, "messageType must not be empty");
        Assert.hasText(eppn, "eppn must not be empty");
        Assert.hasText(json, "json must not be empty");

        jmsTemplate.send(handlerQueue, session -> {
            Message message = session.createTextMessage(json);
            message.setStringProperty("messageType", messageType); // The message type, "CREATE_REGISTRATION_REQUEST" for example
            message.setStringProperty("eppn", eppn); // End user eppn
            message.setStringProperty("schemaVersion", schemaVersion); // Schema version used
            // get the reply to our own queue. temporary queues (Request-Reply pattern) could also be used by removing this
            // and using jmsTemplate.sendAndReceive(...).
            message.setJMSReplyTo(new ActiveMQQueue(organisationQueue));
            return message;
        });
    }

    @Transactional
    @Override
    public List<Message> receive() {
        /** you could also use Spring's MessageConverter to map the JSON directly to a result class like this:
         * (YourResponseClass) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
         *
         * @see ExampleApplicationConfig#jacksonJmsMessageConverter()
         **/

        return jmsTemplate.execute(session -> {
            try (final MessageConsumer consumer = session.createConsumer(session.createQueue(organisationQueue))) {
                List<Message> messages = new ArrayList<>();
                Message message;
                while ((message = consumer.receive(1000)) != null) {
                    messages.add(message);
                }
                // maybe convert to a proper response object?
                //return jmsTemplate.getMessageConverter().fromMessage(message);
                return messages;
            }
        }, true);
    }
}
