package fi.uta.ristiinopiskelu.handler.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test methods are executed in a specific order so that the document to be updated is found from index
 * also the index is deleted when all the tests have been run
 */
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class OrganisationEntityRepositoryTest extends RunListener {

    @Autowired
    private OrganisationRepository organisationRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrganisationEntityRepositoryTest.class);

    @Test
    public void testCreateOrganisation() {
        LocalisedString localisedString = new LocalisedString("Organisaatiotesti","Organisation test","Organisation test");
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId("ORGANISATION-1");
        organisation.setOrganisationName(localisedString);
        organisationRepository.create(organisation);
        organisationRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getOrganisationName())
        );

    }

    @Test
    public void testUpdateOrganisation() {
        LocalisedString localisedString = new LocalisedString("Organisaatiotesti","Organisation test","Organisation test");
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId("ORGANISATION-UPDATE");
        organisation.setOrganisationName(localisedString);
        organisationRepository.create(organisation);

        LocalisedString name = new LocalisedString("Organisaatiotesti muokattu","Organisation test muokattu","Organisation test muokattu");
        OrganisationEntity updatedOrganisation = new OrganisationEntity();
        updatedOrganisation.setOrganisationName(name);

        OrganisationEntity origOrganisation = organisationRepository.findById("ORGANISATION-UPDATE").get();
        organisationRepository.update(origOrganisation);

        organisationRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getOrganisationName())
        );

    }

    @Test
    public void testDeleteOrganisation() {
        LocalisedString localisedString = new LocalisedString("Organisaatiotesti","Organisation test","Organisation test");
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId("ORGANISATION-DELETE");
        organisation.setOrganisationName(localisedString);
        organisationRepository.create(organisation);

        organisationRepository.deleteById("ORGANISATION-DELETE");
        organisationRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getOrganisationName())
        );

    }

}
