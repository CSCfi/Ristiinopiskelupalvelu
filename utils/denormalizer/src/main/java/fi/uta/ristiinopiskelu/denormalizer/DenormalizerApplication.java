package fi.uta.ristiinopiskelu.denormalizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DenormalizerApplication {

    @Autowired
    private RealisationEntityDenormalizer realisationEntityDenormalizer;
    
    public static void main(String[] args) {
        SpringApplication.run(DenormalizerApplication.class, args);
        System.exit(0);
    }

    @ConditionalOnProperty(name = "general.runner-enabled", havingValue = "true")
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> realisationEntityDenormalizer.denormalize();
    }
}
