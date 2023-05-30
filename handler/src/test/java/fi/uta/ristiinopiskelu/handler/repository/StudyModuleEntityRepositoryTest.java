package fi.uta.ristiinopiskelu.handler.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.HandlerApplication;
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
public class StudyModuleEntityRepositoryTest extends RunListener {

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    private static final Logger logger = LoggerFactory.getLogger(StudyModuleEntityRepositoryTest.class);

    @Test
    public void testCreateStudyModule() {

        StudyModuleEntity studyModule = new StudyModuleEntity();
        studyModule.setId("HY-CREATE");
        studyModule.setAbbreviation("Tietojärjestelmät");
        studyModule.setCreditsMin(new BigDecimal(5.0));
        studyModule.setCreditsMax(new BigDecimal(10.0));
        studyModule.setStudyElementIdentifierCode("TIETS2");
        studyModule.setAbbreviation("TIETS2");
        studyModuleRepository.create(studyModule);
        studyModuleRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

    @Test
    public void testUpdateStudyModule() {

        StudyModuleEntity studyModule = new StudyModuleEntity();
        studyModule.setId("HY-UPDATE");
        studyModule.setAbbreviation("Tietojärjestelmät");
        studyModule.setCreditsMin(new BigDecimal(5.0));
        studyModule.setCreditsMax(new BigDecimal(10.0));
        studyModule.setStudyElementIdentifierCode("TIETS2");
        studyModule.setAbbreviation("TIETS2");
        studyModuleRepository.create(studyModule);

        StudyModuleEntity studyModule2 = studyModuleRepository.findById("HY-UPDATE").get();
        studyModule2.setAbbreviation("Tietojärjestelmät MUOKATTU");
        studyModuleRepository.update(studyModule2);
        studyModuleRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

    @Test
    public void testDeleteStudyModule() {

        StudyModuleEntity studyModule = new StudyModuleEntity();
        studyModule.setId("HY-DELETE");
        studyModule.setAbbreviation("Tietojärjestelmät");
        studyModule.setCreditsMin(new BigDecimal(5.0));
        studyModule.setCreditsMax(new BigDecimal(10.0));
        studyModule.setStudyElementIdentifierCode("TIETS2");
        studyModule.setAbbreviation("TIETS2");
        studyModuleRepository.create(studyModule);

        studyModuleRepository.deleteById("HY-DELETE");
        studyModuleRepository.findAll().forEach(
                item -> logger.info("tulos: "+item.getAbbreviation())
        );

    }

}
