package fi.uta.ristiinopiskelu.handler.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class CourseUnitServiceImplIntegrationTest {

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RealisationService realisationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testDenormalisation_denormalizedRealisationsStillPersistentAfterCourseUnitUpdate_shouldSucceed() throws JsonProcessingException {
        OrganisationEntity organisationEntity = EntityInitializer.getOrganisationEntity("UEF", "uef-queue",
            new LocalisedString("uef", null, null), 8);
        organisationRepository.create(organisationEntity);

        Organisation organisation = DtoInitializer.getOrganisation("UEF", "UEF");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyElementReference cuReference = new StudyElementReference("CU1", "UEF", StudyElementType.COURSE_UNIT);

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("REAL1", "UEF",
            new LocalisedString("test", null, null), Collections.singletonList(cuReference), null,
            Collections.singletonList(organisationReference));

        CourseUnitWriteDTO courseUnit = new CourseUnitWriteDTO();
        courseUnit.setStudyElementId("CU1");
        courseUnit.setOrganisationReferences(Collections.singletonList(organisationReference));
        courseUnit.setName(new LocalisedString("test", null, null));
        courseUnit.setRealisations(Collections.singletonList(realisation));
        courseUnit.setStatus(StudyStatus.ACTIVE);

        courseUnitService.createAll(Collections.singletonList(courseUnit), "UEF");

        CourseUnitEntity courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(1, courseUnitEntity.getRealisations().size());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntity.getStatus());
        assertEquals("test", courseUnitEntity.getName().getValue("fi"));
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation.getRealisationId()))
        ));

        // check that the realisation is found by study element reference aand it matches
        List<RealisationEntity> realisationEntities = realisationService.findByStudyElementReference("CU1", "UEF");
        assertNotNull(realisationEntities);
        assertEquals(1, realisationEntities.size());
        assertEquals(realisation.getRealisationId(), realisationEntities.get(0).getRealisationId());

        String updateMessage =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"CU1\",\n" +
                "        \"status\": \"ARCHIVED\"\n" +
                "   }\n" +
                "}";

        JsonNode updateJsonNode = objectMapper.readTree(updateMessage);
        courseUnitService.update(updateJsonNode.get("courseUnit"), "UEF");

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(1, courseUnitEntity.getRealisations().size());
        assertEquals(StudyStatus.ARCHIVED, courseUnitEntity.getStatus());
        assertEquals("test", courseUnitEntity.getName().getValue("fi"));
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation.getRealisationId()))
        ));

        realisationEntities = realisationService.findByStudyElementReference("CU1", "UEF");
        assertNotNull(realisationEntities);
        assertEquals(1, realisationEntities.size());
        assertEquals(realisation.getRealisationId(), realisationEntities.get(0).getRealisationId());

        updateMessage =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"CU1\",\n" +
                "        \"status\": \"ARCHIVED\",\n" +
                "        \"realisations\": null\n" +
                "   }\n" +
                "}";

        updateJsonNode = objectMapper.readTree(updateMessage);
        courseUnitService.update(updateJsonNode.get("courseUnit"), "UEF");

        courseUnitEntity = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).get();

        assertNotNull(courseUnitEntity);
        assertEquals(1, courseUnitEntity.getRealisations().size());
        assertEquals(StudyStatus.ARCHIVED, courseUnitEntity.getStatus());
        assertEquals("test", courseUnitEntity.getName().getValue("fi"));
        assertThat(courseUnitEntity.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(realisation.getRealisationId()))
        ));

        realisationEntities = realisationService.findByStudyElementReference("CU1", "UEF");
        assertNotNull(realisationEntities);
        assertEquals(1, realisationEntities.size());
        assertEquals(realisation.getRealisationId(), realisationEntities.get(0).getRealisationId());
    }
}
