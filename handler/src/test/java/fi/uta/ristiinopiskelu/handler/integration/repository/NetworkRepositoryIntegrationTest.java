package fi.uta.ristiinopiskelu.handler.integration.repository;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class NetworkRepositoryIntegrationTest {

    @Autowired
    private NetworkRepository networkRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetworkRepositoryIntegrationTest.class);

    @Test
    public void testCreateNetwork() {
        LocalisedStringEntity name = new LocalisedStringEntity("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-1");
        network.setName(name);
        networkRepository.create(network);
        networkRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );

    }

    @Test
    public void testUpdateOptimisticLockNetwork() {
        LocalisedStringEntity name = new LocalisedStringEntity("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-UPDATE-1");
        network.setName(name);
        networkRepository.create(network);

        LocalisedStringEntity name2 = new LocalisedStringEntity("Testiverkosto MUOKATTU","Testiverkosto MODIFIED","Testiverkosto MOD");
        NetworkEntity network2 = networkRepository.findById("NETWORK-UPDATE-1").get();
        network2.setName(name2);

        VersionConflictException exception = Assertions.assertThrows(VersionConflictException.class,
            () -> networkRepository.create(network2));
        assertTrue(exception.getCause() instanceof ElasticsearchException);
        ElasticsearchException elasticsearchException = (ElasticsearchException) exception.getCause();
        assertEquals(HttpStatus.SC_CONFLICT, elasticsearchException.status());
    }

    @Test
    public void testUpdateNetwork() {
        LocalisedStringEntity name = new LocalisedStringEntity("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-UPDATE-2");
        network.setName(name);
        networkRepository.create(network);

        LocalisedStringEntity name2 = new LocalisedStringEntity("Testiverkosto MUOKATTU","Testiverkosto","Testiverkosto");
        NetworkEntity updatedEntity = new NetworkEntity();
        updatedEntity.setName(name2);

        NetworkEntity originalNetwork = networkRepository.findById("NETWORK-UPDATE-2").get();

        networkRepository.update(originalNetwork);
        networkRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );

    }

    @Test
    public void testDeleteNetwork() {
        LocalisedStringEntity name = new LocalisedStringEntity("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-DELETE");
        network.setName(name);
        networkRepository.create(network);

        networkRepository.deleteById("NETWORK-DELETE");
        networkRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );
    }
}
