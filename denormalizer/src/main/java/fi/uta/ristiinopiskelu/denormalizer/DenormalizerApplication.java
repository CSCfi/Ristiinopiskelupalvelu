package fi.uta.ristiinopiskelu.denormalizer;

import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {
    "fi.uta.ristiinopiskelu.denormalizer"
})
@Import({EsConfig.class, MapperConfig.class})
public class DenormalizerApplication implements CommandLineRunner {

    @Autowired
    private RealisationEntityDenormalizer realisationEntityDenormalizer;
    
    public static void main(String[] args) {
        SpringApplication.run(DenormalizerApplication.class, args);
        System.exit(0);
    }

    @Override
    public void run(String... args) throws Exception {
        realisationEntityDenormalizer.denormalize();
    }
}
