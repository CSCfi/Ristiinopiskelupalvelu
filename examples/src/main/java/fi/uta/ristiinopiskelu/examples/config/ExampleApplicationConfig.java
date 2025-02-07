package fi.uta.ristiinopiskelu.examples.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.examples.message.CreateRegistrationRequest;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EnableJms
@Configuration
public class ExampleApplicationConfig {

    private static final String KEYSTORE_FILE = "classpath:keystore.p12";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String TRUSTSTORE_FILE = "classpath:truststore.ts";
    private static final String TRUSTSTORE_PASWORD = "password";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        return objectMapper;
    }

    @Bean
    public JmsTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new JmsTransactionManager(connectionFactory);
    }

    @Bean
    public JmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                   DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                                   JmsTransactionManager jmsTransactionManager) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setTransactionManager(jmsTransactionManager);
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    /**
     * Custom configuration for ActiveMQConnectionFactory in order to use SSL. Client private/public key pair in keystore, CA certificate in trust store.
     * Alternatively add key pair and CA to Java's default key/trust store. In this example Key/truststore files should be placed in src/main/resources.
     * This is probably not the best idea when doing this in production.
     *
     * It's also a good idea to use a connection pool with real life cases, see Spring manuals & org.messaginghub:pooled-jms artifact.
     * @param brokerUrl
     * @return
     */
    @Primary
    @Bean
    public ConnectionFactory jmsConnectionFactory(@Value("${spring.artemis.broker-url}") String brokerUrl) {
        Assert.hasText(brokerUrl, "Broker url must be specified with pattern <HOST>:<PORT>");

        String[] splittedBrokerUrl = brokerUrl.split(":");
        if(splittedBrokerUrl.length < 2) {
            throw new IllegalArgumentException("No port specified in broker-url '" + brokerUrl + "'");
        }

        String host = splittedBrokerUrl[0];
        String port = splittedBrokerUrl[1];

        Map<String, Object> transportParameters = new HashMap<>(7);
        transportParameters.put(TransportConstants.HOST_PROP_NAME, host);
        transportParameters.put(TransportConstants.PORT_PROP_NAME, port);
        transportParameters.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
        transportParameters.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, TRUSTSTORE_FILE);
        transportParameters.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, TRUSTSTORE_PASWORD);
        transportParameters.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, KEYSTORE_FILE);
        transportParameters.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, KEYSTORE_PASSWORD);
        return new ActiveMQConnectionFactory(false, new TransportConfiguration(NettyConnectorFactory.class.getName(), transportParameters));
    }

    /**
     * Timeout for receiving replies is set to 30 seconds here.
     * @return
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setReceiveTimeout(30000);
        return jmsTemplate;
    }


    // TODO: you could also configure this message converter in order to automagically convert JSON message body to POJO via Jackson.
    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        Map<String, Class<?>> typeIds = Collections.singletonMap("CREATE_REGISTRATION_REQUEST", CreateRegistrationRequest.class);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("messageType");
        converter.setTargetType(org.springframework.jms.support.converter.MessageType.TEXT);
        converter.setObjectMapper(objectMapper);
        converter.setTypeIdMappings(typeIds);
        return converter;
    }

    /**
     * RestTemplate configuration that uses client SSL certificate and adds the required "eppn" header to every request. Certificates
     * are required with REST too.
     * @param builder
     * @return
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws IOException, UnrecoverableKeyException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // load our SSL client key/certificate pair. REST API won't work without it.
        SSLContext sslContext = SSLContextBuilder.create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray(), KEYSTORE_PASSWORD.toCharArray())
            .build();

        // note: this always forces ssl regardless of "http" for example
        BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(httpMethod -> new SSLConnectionSocketFactory(sslContext));

        HttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        RestTemplate restTemplate = builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client)).build();

        // add "eppn" header to all queries as it is required. It's supposed to be the EPPN of the actual user executing the query.
        restTemplate.getInterceptors().add((httpRequest, bytes, clientHttpRequestExecution) -> {
            httpRequest.getHeaders().set("eppn", "test@test.fi");
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        });

        return restTemplate;
    }
}
