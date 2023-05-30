package fi.csc.ristiinopiskelu.admin.config;


import fi.csc.ristiinopiskelu.admin.controller.interceptor.AuditLoggingInterceptor;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Profile("!dev")
@Configuration
@Import({EsConfig.class, AdminUiMapperConfig.class, MessagingConfig.class})
public class ApplicationConfig implements WebMvcConfigurer {

    @Bean
    public AuditLoggingInterceptor auditLoggingInterceptor() {
        return new AuditLoggingInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor());
    }

    @Value("${tomcat.ajp.port}")
    private int ajpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server -> {
            if (server instanceof TomcatServletWebServerFactory) {
                server.addAdditionalTomcatConnectors(redirectConnector());
            }
        };
    }
    private Connector redirectConnector() {
        Connector connector = new Connector("AJP/1.3");
        ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setSecretRequired(false);
        ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setAllowedRequestAttributesPattern(".*");

        try {
            ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setAddress(InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e){
            throw new IllegalStateException(e.getMessage());
        }

        connector.setScheme("http");
        connector.setPort(ajpPort);
        connector.setRedirectPort(8443);
        connector.setSecure(false);
        connector.setAllowTrace(false);
        return connector;
    }
}
