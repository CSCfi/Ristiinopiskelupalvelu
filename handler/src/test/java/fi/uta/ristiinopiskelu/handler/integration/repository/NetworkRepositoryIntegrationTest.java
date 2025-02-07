package fi.uta.ristiinopiskelu.handler.integration.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
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

        DataAccessResourceFailureException exception = Assertions.assertThrows(DataAccessResourceFailureException.class,
            () -> networkRepository.create(network2));
        assertTrue(exception.getCause().getCause() instanceof ResponseException);
        ResponseException responseException = (ResponseException) exception.getCause().getCause();
        assertEquals(HttpStatus.SC_CONFLICT, responseException.getResponse().getStatusLine().getStatusCode());
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
