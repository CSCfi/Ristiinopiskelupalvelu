package fi.uta.ristiinopiskelu.handler.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.rest.RestStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.RestStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test methods are executed in a specific order so that the document to be updated is found from index
 * also the index is deleted when all the tests have been run
 */
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class NetworkEntityRepositoryTest extends RunListener {

    @Autowired
    private NetworkRepository networkRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetworkEntityRepositoryTest.class);

    @Test
    public void testCreateNetwork() {
        LocalisedString name = new LocalisedString("Testiverkosto","Testiverkosto","Testiverkosto");
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
        LocalisedString name = new LocalisedString("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-UPDATE-1");
        network.setName(name);
        networkRepository.create(network);

        LocalisedString name2 = new LocalisedString("Testiverkosto MUOKATTU","Testiverkosto MODIFIED","Testiverkosto MOD");
        NetworkEntity network2 = networkRepository.findById("NETWORK-UPDATE-1").get();
        network2.setName(name2);

        RestStatusException exception = Assertions.assertThrows(RestStatusException.class,
            () -> networkRepository.create(network2));
        assertTrue(exception.getCause() instanceof ElasticsearchStatusException);
        ElasticsearchStatusException elasticsearchStatusException = (ElasticsearchStatusException) exception.getCause();
        assertEquals(RestStatus.CONFLICT, elasticsearchStatusException.status());
    }

    @Test
    public void testUpdateNetwork() {
        LocalisedString name = new LocalisedString("Testiverkosto","Testiverkosto","Testiverkosto");
        NetworkEntity network = new NetworkEntity();
        network.setId("NETWORK-UPDATE-2");
        network.setName(name);
        networkRepository.create(network);

        LocalisedString name2 = new LocalisedString("Testiverkosto MUOKATTU","Testiverkosto","Testiverkosto");
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
        LocalisedString name = new LocalisedString("Testiverkosto","Testiverkosto","Testiverkosto");
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
