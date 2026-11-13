package fi.csc.ristiinopiskelu.admin.config;

import fi.csc.ristiinopiskelu.admin.controller.interceptor.AuditLoggingInterceptor;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Profile("!dev")
@Configuration
@Import({EsConfig.class, MessagingConfig.class})
public class ApplicationConfig implements WebMvcConfigurer {

    @Bean
    public AuditLoggingInterceptor auditLoggingInterceptor() {
        return new AuditLoggingInterceptor();
    }

    @Bean
    public JacksonModule trimmingStringModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        return module;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor());
    }

    @Value("${tomcat.ajp.port}")
    private int ajpPort;

    @Value("${tomcat.ajp.secret-required:false}")
    private boolean ajpSecretRequired;

    @Value("${tomcat.ajp.secret:}")
    private String ajpSecret;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server -> server.addAdditionalConnectors(ajpConnector());
    }

    private Connector ajpConnector() {
        if(ajpSecretRequired && !StringUtils.hasText(ajpSecret)) {
            throw new IllegalStateException("tomcat.ajp.secret must be configured when tomcat.ajp.secret-required is true");
        }

        Connector connector = new Connector("AJP/1.3");
        AbstractAjpProtocol<?> protocol = (AbstractAjpProtocol<?>) connector.getProtocolHandler();

        protocol.setSecretRequired(ajpSecretRequired);

        if(StringUtils.hasText(ajpSecret)) {
            protocol.setSecret(ajpSecret);
        }

        protocol.setAllowedRequestAttributesPattern(".*");

        try {
            protocol.setAddress(InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }

        connector.setScheme("https");
        connector.setPort(ajpPort);
        connector.setRedirectPort(8443);
        connector.setSecure(true);
        connector.setAllowTrace(false);
        return connector;
    }
}
