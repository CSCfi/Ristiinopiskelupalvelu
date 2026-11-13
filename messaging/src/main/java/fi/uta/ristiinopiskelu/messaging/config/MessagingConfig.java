package fi.uta.ristiinopiskelu.messaging.config;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.messaging.MessageTypeClassProvider;
import fi.uta.ristiinopiskelu.messaging.jms.VersionedMessageTypeAwareDelegatingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MessageConverter;

@EnableJms
@Configuration
@ComponentScan(basePackages = {
    "fi.uta.ristiinopiskelu.messaging",
    "fi.uta.ristiinopiskelu.messaging.schema"
})
public class MessagingConfig {

    @Bean
    public MessageConverter mappingJackson2MessageConverter(JsonMapper jsonMapper, MessageTypeClassProvider messageTypeClassProvider) {
        return new VersionedMessageTypeAwareDelegatingMessageConverter(jsonMapper, messageTypeClassProvider);
    }
}
