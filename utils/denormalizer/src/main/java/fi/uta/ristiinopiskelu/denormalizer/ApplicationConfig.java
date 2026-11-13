package fi.uta.ristiinopiskelu.denormalizer;

import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ EsConfig.class })
public class ApplicationConfig {
}
