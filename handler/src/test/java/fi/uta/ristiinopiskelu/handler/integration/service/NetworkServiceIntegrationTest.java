package fi.uta.ristiinopiskelu.handler.integration.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LanguageEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.VersionConflictException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class NetworkServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceIntegrationTest.class);

    @Autowired
    private NetworkService networkService;

    @Test
    public void testUpdate_shouldUpdateNetwork() {
        NetworkEntity origEntity = new NetworkEntity();
        origEntity.setId("ABCD-NETWORK-1");
        origEntity.setName(new LocalisedStringEntity("Testiverkosto","TestiverkostoEN","TestiverkostoSV"));
        networkService.create(origEntity);
        origEntity = networkService.findById(origEntity.getId()).get();

        LocalisedStringEntity newName = new LocalisedStringEntity("Testiverkosto MOD","TestiverkostoEN MOD","TestiverkostoSV MOD");
        origEntity.setId("ABCD-NETWORK-1");
        origEntity.setName(newName);

        networkService.update(origEntity);

        NetworkEntity updatedEntity = networkService.findById("ABCD-NETWORK-1").orElse(null);
        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getName().getValue(LanguageEntity.FI), newName.getValue(LanguageEntity.FI));
        assertEquals(2L, updatedEntity.getVersion());
    }
    
    @Test
    public void testUpdate_shouldNotUpdateBecauseOptimisticLock() {
        LocalisedStringEntity origName = new LocalisedStringEntity("Testiverkosto","TestiverkostoEN","TestiverkostoSV");
        NetworkEntity origEntity = new NetworkEntity();
        origEntity.setId("ABCD-NETWORK-2");
        origEntity.setName(origName);

        final NetworkEntity created = (NetworkEntity) networkService.create(origEntity).get(0).getCurrent();

        LocalisedStringEntity newName = new LocalisedStringEntity("Testiverkosto MOD","TestiverkostoEN MOD","TestiverkostoSV MOD");
        created.setVersion(0L); // Set version one step lower so VersionConflictEngineException should be thrown
        created.setId("ABCD-NETWORK-2");
        created.setName(newName);

        UpdateFailedException exception = Assertions.assertThrows(UpdateFailedException.class,
            () -> networkService.update(created));
        assertTrue(exception.getCause() instanceof VersionConflictException);
        assertTrue(exception.getCause().getCause() instanceof ElasticsearchException);
        ElasticsearchException elasticsearchException = (ElasticsearchException) exception.getCause().getCause();
        assertEquals(HttpStatus.SC_CONFLICT, elasticsearchException.status());
                
        NetworkEntity updatedEntity = networkService.findById("ABCD-NETWORK-2").orElse(null);
        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getName().getValue(LanguageEntity.FI), origName.getValue(LanguageEntity.FI));
        assertNotEquals(updatedEntity.getName().getValue(LanguageEntity.FI), newName.getValue(LanguageEntity.FI));
        assertEquals(1L, updatedEntity.getVersion());
    }
}
