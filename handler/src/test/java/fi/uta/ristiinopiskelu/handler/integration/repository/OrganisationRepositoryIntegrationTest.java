package fi.uta.ristiinopiskelu.handler.integration.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class OrganisationRepositoryIntegrationTest {

    @Autowired
    private OrganisationRepository organisationRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrganisationRepositoryIntegrationTest.class);

    @Test
    public void testCreateOrganisation() {
        LocalisedStringEntity localisedString = new LocalisedStringEntity("Organisaatiotesti","Organisation test","Organisation test");
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
        LocalisedStringEntity localisedString = new LocalisedStringEntity("Organisaatiotesti","Organisation test","Organisation test");
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId("ORGANISATION-UPDATE");
        organisation.setOrganisationName(localisedString);
        organisationRepository.create(organisation);

        LocalisedStringEntity name = new LocalisedStringEntity("Organisaatiotesti muokattu","Organisation test muokattu","Organisation test muokattu");
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
        LocalisedStringEntity localisedString = new LocalisedStringEntity("Organisaatiotesti","Organisation test","Organisation test");
        OrganisationEntity organisation = new OrganisationEntity();
        organisation.setId("ORGANISATION-DELETE");
        organisation.setOrganisationName(localisedString);
        organisationRepository.create(organisation);

        organisationRepository.deleteById("ORGANISATION-DELETE");
        organisationRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getOrganisationName())
        );

    }

    @Test
    public void testSearch_unpagedSearchReturnsResultsPastElasticsearchMaxResultWindow_shouldSucceed() {
        int documentCount = 10001;
        List<OrganisationEntity> organisations = IntStream.range(0, documentCount)
            .mapToObj(index -> {
                OrganisationEntity organisation = new OrganisationEntity();
                organisation.setId("UNPAGED-" + index);
                return organisation;
            })
            .toList();

        organisationRepository.saveAll(organisations);

        assertEquals(documentCount, organisationRepository.search(
            QueryBuilders.matchAll().build()._toQuery(), null).size());
        assertEquals(documentCount, organisationRepository.findAll(Pageable.unpaged()).getTotalElements());
        assertEquals(documentCount, StreamSupport.stream(organisationRepository.findAll().spliterator(), false).count());
        assertEquals(documentCount, StreamSupport.stream(organisationRepository.findAllById(
            organisations.stream().map(OrganisationEntity::getId).toList()).spliterator(), false).count());
    }

    @Test
    public void testSearch_unpagedSearchPreservesSortAcrossScrollBatches_shouldSucceed() {
        int documentCount = 1001;
        List<OrganisationEntity> organisations = IntStream.range(0, documentCount)
            .mapToObj(index -> {
                OrganisationEntity organisation = new OrganisationEntity();
                organisation.setId("SORTED-" + index);
                organisation.setOrganisationIdentifier("%04d".formatted(index));
                return organisation;
            })
            .toList();

        organisationRepository.saveAll(organisations);

        List<String> actualIdentifiers = organisationRepository.search(
                QueryBuilders.matchAll().build()._toQuery(),
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "organisationIdentifier")))
            .stream()
            .map(OrganisationEntity::getOrganisationIdentifier)
            .toList();
        List<String> expectedIdentifiers = IntStream.iterate(documentCount - 1, index -> index >= 0, index -> index - 1)
            .mapToObj(index -> "%04d".formatted(index))
            .toList();

        assertEquals(expectedIdentifiers, actualIdentifiers);
    }
}
