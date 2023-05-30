package fi.uta.ristiinopiskelu.handler.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test methods are executed in a specific order so that the document to be updated is found from index
 * also the index is deleted when all the tests have been run
 */
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class CourseUnitEntityRepositoryTest extends RunListener {

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    private static final Logger logger = LoggerFactory.getLogger(CourseUnitEntityRepositoryTest.class);

    @Test
    public void testCreateCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-TIETS2");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );
    }

    @Test
    public void testCreateDuplicateCourseUnit_shouldFail() {
        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-TIETS2");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit, IndexQuery.OpType.CREATE);
        assertThrows(DataIntegrityViolationException.class, () -> courseUnitRepository.create(courseUnit, IndexQuery.OpType.CREATE));
    }

    @Test
    public void testUpdateCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-UPDATE");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);

        CourseUnitEntity courseUnit2 = courseUnitRepository.findById("HY-UPDATE").get();
        courseUnit.setAbbreviation("Tietojärjestelmät MUOKATTU");
        courseUnitRepository.update(courseUnit2);
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

    @Test
    public void testDeleteCourseUnit_shouldSucceed() {

        CourseUnitEntity courseUnit = new CourseUnitEntity();
        courseUnit.setId("HY-DELETE");
        courseUnit.setAbbreviation("Tietojärjestelmät");
        courseUnit.setCreditsMin(new BigDecimal(5.0));
        courseUnit.setCreditsMax(new BigDecimal(10.0));
        courseUnit.setStudyElementIdentifierCode("TIETS2");
        courseUnit.setAbbreviation("TIETS2");
        courseUnitRepository.create(courseUnit);

        courseUnitRepository.deleteById("HY-DELETE");
        courseUnitRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

}
