package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.HandlerApplication;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = HandlerApplication.class)
public class RegistrationServiceTest {

    
}
