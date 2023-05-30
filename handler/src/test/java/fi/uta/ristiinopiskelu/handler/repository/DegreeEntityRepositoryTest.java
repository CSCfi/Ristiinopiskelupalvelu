package fi.uta.ristiinopiskelu.handler.repository;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.entity.DegreeEntity;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.DegreeRepository;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;


/**
 * Test methods are executed in a specific order so that the document to be updated is found from index
 * also the index is deleted when all the tests have been run
 */
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class DegreeEntityRepositoryTest extends RunListener {

    @Autowired
    private DegreeRepository degreeRepository;

    private static final Logger logger = LoggerFactory.getLogger(DegreeEntityRepositoryTest.class);

    @Test
    public void testCreateDegree() {

        DegreeEntity degree = new DegreeEntity();
        degree.setId("HY-TIETS2");
        degree.setName(new LocalisedString("Tietojärjestelmät", "Tietojärjestelmät", "Tietojärjestelmät"));
        degree.setCreditsMin(new BigDecimal(5.0));
        degree.setCreditsMax(new BigDecimal(10.0));
        degree.setStudyElementIdentifierCode("TIETS2");
        degree.setAbbreviation("TIETS2");
        degreeRepository.create(degree);
        degreeRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );

    }

    @Test
    public void testUpdateDegree() {

        DegreeEntity degree = new DegreeEntity();
        degree.setId("HY-UPDATE");
        degree.setName(new LocalisedString("Tietojärjestelmät", "Tietojärjestelmät", "Tietojärjestelmät"));
        degree.setCreditsMin(new BigDecimal(5.0));
        degree.setCreditsMax(new BigDecimal(10.0));
        degree.setStudyElementIdentifierCode("TIETS2");
        degree.setAbbreviation("TIETS2");
        degreeRepository.create(degree);

        DegreeEntity degree2 = degreeRepository.findById("HY-UPDATE").get();
        degree2.setName(new LocalisedString("Tietojärjestelmät muokattu", "Tietojärjestelmät muokattu", "Tietojärjestelmät muokattu"));
        degreeRepository.update(degree2);
        degreeRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );

    }

    @Test
    public void testDeleteDegree() {
        DegreeEntity degree = new DegreeEntity();
        degree.setId("HY-DELETE");
        degree.setName(new LocalisedString("Tietojärjestelmät", "Tietojärjestelmät", "Tietojärjestelmät"));
        degree.setCreditsMin(new BigDecimal(5.0));
        degree.setCreditsMax(new BigDecimal(10.0));
        degree.setStudyElementIdentifierCode("TIETS2");
        degree.setAbbreviation("TIETS2");
        degreeRepository.create(degree);

        degreeRepository.deleteById("HY-DELETE");
        degreeRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getName())
        );

    }

}
