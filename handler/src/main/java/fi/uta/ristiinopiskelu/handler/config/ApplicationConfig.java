package fi.uta.ristiinopiskelu.handler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.handler.controller.interceptor.AuditLoggingInterceptor;
import fi.uta.ristiinopiskelu.messaging.config.MessagingConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.springframework.beans.BeansException;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Configuration
@Import({EsConfig.class, MapperConfig.class, MessagingConfig.class})
public class ApplicationConfig implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.forEach(converter -> {
            if (converter.getClass().isAssignableFrom(MappingJackson2HttpMessageConverter.class)) {
                MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = (MappingJackson2HttpMessageConverter) converter;
                mappingJackson2HttpMessageConverter.setObjectMapper((ObjectMapper) applicationContext.getBean("objectMapper"));
            }
        });
    }

    @Bean
    public AuditLoggingInterceptor auditLoggingInterceptor() {
        return new AuditLoggingInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor());
    }

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

        connector.setScheme("https");
        connector.setPort(8009);
        connector.setRedirectPort(8443);
        connector.setSecure(true);
        connector.setAllowTrace(false);
        return connector;
    }
}
