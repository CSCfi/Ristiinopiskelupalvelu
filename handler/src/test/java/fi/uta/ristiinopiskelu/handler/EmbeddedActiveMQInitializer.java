package fi.uta.ristiinopiskelu.handler;

import fi.uta.ristiinopiskelu.tracker.plugin.MessageTrackingActiveMQServerPlugin;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedActiveMQInitializer implements AfterEachCallback, BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedActiveMQInitializer.class);

    private static EmbeddedActiveMQ broker = null;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if(broker == null) {
            Configuration config = new ConfigurationImpl();
            config.addAcceptorConfiguration("in-vm", "vm://0");
            config.setSecurityEnabled(false);
            config.setPersistenceEnabled(false);
            config.setPopulateValidatedUser(true);

            config.registerBrokerPlugin(new MessageTrackingActiveMQServerPlugin());
            
            broker = new EmbeddedActiveMQ();
            broker.setConfiguration(config);
            broker.start();
        }
    }

    @Override
    public void close() throws Throwable {
        broker.stop();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        logger.info("******************** Running test {} ********************", context.getDisplayName());
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ActiveMQServerControl control = broker.getActiveMQServer().getActiveMQServerControl();

        for (String queue : control.getQueueNames()) {
            if (!queue.equals("handler")) {
                logger.info("Destroying queue '{}'", queue);
                control.destroyQueue(queue, true, true);
            }
        }

        logger.info("******************** Finished test {} ********************", context.getDisplayName());
    }
}
