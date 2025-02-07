package fi.uta.ristiinopiskelu.handler.integration.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.exception.UpdateFailedException;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        final NetworkEntity created = (NetworkEntity) networkService.create(origEntity).get(0).getCurrent();

        LocalisedString newName = new LocalisedString("Testiverkosto MOD","TestiverkostoEN MOD","TestiverkostoSV MOD");
        created.setVersion(0L); // Set version one step lower so VersionConflictEngineException should be thrown
        created.setId("ABCD-NETWORK-2");
        created.setName(newName);

        UpdateFailedException exception = Assertions.assertThrows(UpdateFailedException.class,
            () -> networkService.update(created));
        assertTrue(exception.getCause() instanceof DataAccessResourceFailureException);
        assertTrue(exception.getCause().getCause().getCause() instanceof ResponseException);
        ResponseException responseException = (ResponseException) exception.getCause().getCause().getCause();
        assertEquals(HttpStatus.SC_CONFLICT, responseException.getResponse().getStatusLine().getStatusCode());
                
        NetworkEntity updatedEntity = networkService.findById("ABCD-NETWORK-2").orElse(null);
        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getName().getValue(Language.FI), origName.getValue(Language.FI));
        assertNotEquals(updatedEntity.getName().getValue(Language.FI), newName.getValue(Language.FI));
        assertEquals(1L, updatedEntity.getVersion());
    }
}
