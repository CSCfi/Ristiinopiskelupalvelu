package fi.uta.ristiinopiskelu.handler.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class RealisationServiceImplIntegrationTest {

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testDenormalisation_removeDenormalizedRealisationsFromCourseUnit_shouldSucceed() throws JsonProcessingException {

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU1", "UEF",
            null, new LocalisedString("test", null, null));
        courseUnitRepository.create(courseUnitEntity);

        StudyElementReference cuReference = new StudyElementReference(courseUnitEntity.getStudyElementId(),
            courseUnitEntity.getOrganizingOrganisationId(), StudyElementType.COURSE_UNIT);

        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("UEF", "uef-queue",
            new LocalisedString("uef", null, null), 8);
        organisationRepository.create(organisationEntity);

        Organisation organisation = DtoInitializer.getOrganisation("UEF", "UEF");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("REAL1", "UEF",
            Collections.singletonList(cuReference), null);

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("REAL2", "UEF",
            Collections.singletonList(cuReference), null);

        realisation.setOrganisationReferences(Collections.singletonList(organisationReference));
        realisation = realisationService.create(realisation);

        realisation2.setOrganisationReferences(Collections.singletonList(organisationReference));
        realisation2 = realisationService.create(realisation2);

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(2, courseUnitEntity.getRealisations().size());
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation.getRealisationId())),
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));

        String updateMessage =
            "{\n" +
                "    \"realisation\": {\n" +
                "        \"realisationId\": \"REAL1\",\n" +
                "        \"studyElementReferences\": []\n" +
                "   }\n" +
                "}";

        JsonNode updateJsonNode = objectMapper.readTree(updateMessage);

        realisation = realisationService.update(updateJsonNode.get("realisation"), "UEF");

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitEntity.getStudyElementId(), courseUnitEntity.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(1, courseUnitEntity.getRealisations().size());
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation2.getRealisationId()))
        ));
    }
}
