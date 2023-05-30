package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.registration.RegistrationWriteDTO;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class MessageSchemaServiceTest {

    @Autowired
    private MessageSchemaService messageSchemaService;

    @Test
    public void testConvertingNetworkV8ToV9_shouldSucceed() {
        fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network v8Network = new fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network();
        v8Network.setPublished(true);

        NetworkWriteDTO converted = messageSchemaService.convertObject(v8Network, NetworkWriteDTO.class);
        assertTrue(converted.isPublished());

        v8Network.setPublished(false);
        converted = messageSchemaService.convertObject(v8Network, NetworkWriteDTO.class);
        assertFalse(converted.isPublished());
    }

    @Test
    public void testConvertingNetworkV9ToV8_shouldSucceed() {
        NetworkWriteDTO currentNetwork = new NetworkWriteDTO();
        currentNetwork.setPublished(true);

        fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network converted = messageSchemaService.convertObject(currentNetwork,
            fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network.class);
        assertTrue(converted.getPublished());

        currentNetwork.setPublished(false);
        converted = messageSchemaService.convertObject(currentNetwork, fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network.class);
        assertFalse(converted.getPublished());
    }

    @Test
    public void testConvertingRegistrationV8ToV9withDefaultConverter_shouldSucceed() {
        fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.Registration v8Registration = new fi.uta.ristiinopiskelu.datamodel.dto.v8.registration.Registration();
        v8Registration.setEnrolmentDateTime(OffsetDateTime.now());
        RegistrationWriteDTO convertedRegistration = messageSchemaService.convertObject(v8Registration, RegistrationWriteDTO.class);
        assertEquals(v8Registration.getEnrolmentDateTime(), convertedRegistration.getEnrolmentDateTime());
    }
}
