package fi.uta.ristiinopiskelu.examples.jms;

import org.apache.activemq.artemis.jms.client.ActiveMQBytesMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.nio.charset.StandardCharsets;

/**
 * This is an example on how to implement a listener that continuously listens to JMS messages from queue.
 * Remove comments from @Component in order to make this work. Note that this will then always consume all messages from the queue
 * as they arrive, so ExampleQueueController#receive() probably doesn't make sense anymore.
 *
 * @see ExampleQueueController
 *
 */
//@Component
public class ExampleListener {

    private static final Logger logger = LoggerFactory.getLogger(ExampleListener.class);

    /**
     * Called when messages are received.
     *
     * It's also possible to use Spring's MessageConverter manually (or automagically) to map the JSON directly to a
     * result class. Manually this can be done like this:
     *
     * (YourResponseClass) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
     *
     * @see ExampleApplicationConfig
     * @param message
     */
    @JmsListener(destination = "${general.organisation-queue}")
    public void onMessage(Message message) {
        if(!(message instanceof ActiveMQBytesMessage)) {
            logger.info("Received unsupported message type: {}", message);
            return;
        }

        try {
            byte[] messageBody = message.getBody(byte[].class);
            logger.info("Received JSON: {}", new String(messageBody, StandardCharsets.UTF_8));
        } catch (JMSException e) {
            logger.error("Error while getting received message JSON", e);
        }
    }
}
