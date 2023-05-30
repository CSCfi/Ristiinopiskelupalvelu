package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.RestStatusException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class NetworkServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceTest.class);

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testUpdate_shouldUpdateNetwork() {
        NetworkEntity origEntity = new NetworkEntity();
        origEntity.setId("ABCD-NETWORK-1");
        origEntity.setName(new LocalisedString("Testiverkosto","TestiverkostoEN","TestiverkostoSV"));
        networkService.create(origEntity);
        origEntity = networkService.findById(origEntity.getId()).get();

        LocalisedString newName = new LocalisedString("Testiverkosto MOD","TestiverkostoEN MOD","TestiverkostoSV MOD");
        origEntity.setId("ABCD-NETWORK-1");
        origEntity.setName(newName);

        networkService.update(origEntity);

        NetworkEntity updatedEntity = networkService.findById("ABCD-NETWORK-1").orElse(null);
        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getName().getValue(Language.FI), newName.getValue(Language.FI));
        assertEquals(2L, updatedEntity.getVersion());
    }
    
    @Test
    public void testUpdate_shouldNotUpdateBecauseOptimisticLock() {
        LocalisedString origName = new LocalisedString("Testiverkosto","TestiverkostoEN","TestiverkostoSV");
        NetworkEntity origEntity = new NetworkEntity();
        origEntity.setId("ABCD-NETWORK-2");
        origEntity.setName(origName);

        final NetworkEntity created = networkService.create(origEntity);

        LocalisedString newName = new LocalisedString("Testiverkosto MOD","TestiverkostoEN MOD","TestiverkostoSV MOD");
        created.setVersion(0L); // Set version one step lower so VersionConflictEngineException should be thrown
        created.setId("ABCD-NETWORK-2");
        created.setName(newName);

        UpdateFailedException exception = Assertions.assertThrows(UpdateFailedException.class,
            () -> networkService.update(created));
        assertTrue(exception.getCause() instanceof RestStatusException);
        RestStatusException restStatusException = (RestStatusException) exception.getCause();
        assertEquals(RestStatus.CONFLICT.getStatus(), restStatusException.getStatus());

        NetworkEntity updatedEntity = networkService.findById("ABCD-NETWORK-2").orElse(null);
        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getName().getValue(Language.FI), origName.getValue(Language.FI));
        assertNotEquals(updatedEntity.getName().getValue(Language.FI), newName.getValue(Language.FI));
        assertEquals(1L, updatedEntity.getVersion());
    }
}
