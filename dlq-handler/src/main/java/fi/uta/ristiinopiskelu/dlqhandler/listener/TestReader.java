package fi.uta.ristiinopiskelu.dlqhandler.listener;

import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
    This component is only for testing purposes when running project locally.
    Component reads from test-queue, so you should have organisation that has test-queue configured for it.
    Listener is configured transactionally and message from queue is read and failed always -> message is delivered to DLQ that can then be handled by dlq-handler
*/
@Component
@Profile("dev")
public class TestReader {
    @JmsListener(destination = "test-queue", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void receiveMessage(final String msg) throws Exception {
        System.out.println("Received:" + msg);
        throw new IllegalStateException();
    }

    @JmsListener(destination = "test2-queue", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void receiveMessage2(final String msg) throws Exception {
        System.out.println("Received:" + msg);
        throw new IllegalStateException();
    }
}
