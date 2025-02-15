package fi.uta.ristiinopiskelu.dlqhandler.config;

import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import jakarta.jms.ConnectionFactory;
import org.simplejavamail.springsupport.SimpleJavaMailSpringSupport;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@Import({EsConfig.class, MapperConfig.class, MessagingConfig.class, SimpleJavaMailSpringSupport.class})
public class ApplicationConfig {

    @Bean
    public JmsTransactionManager jmsTransactionManager(final ConnectionFactory connectionFactory) {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(connectionFactory);
        return jmsTransactionManager;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setTransactionManager(jmsTransactionManager(connectionFactory));
        factory.setSessionTransacted(true);
        return factory;
    }
}
