package fi.uta.ristiinopiskelu.handler.integration.controller.current;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyElementReferenceEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyStatusEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudiesControllerV9StudyModuleIntegrationTest extends AbstractStudiesControllerV9IntegrationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Test
    public void testFindStudyModules_returnsOnlyStudyModulesWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/studymodules";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetworkEntity coopNetwork = createCooperationNetwork("CN-1");

        StudyModuleEntity activeStudyModuleEntity = createStudyModule("SM1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);

        StudyModuleEntity cancelledStudyModuleEntity = createStudyModule("SM2", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);
        cancelledStudyModuleEntity.setStatus(StudyStatusEntity.CANCELLED);
        studyModuleRepository.update(cancelledStudyModuleEntity);

        StudyModuleEntity archivedStudyModuleEntity = createStudyModule("SM3", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);
        archivedStudyModuleEntity.setStatus(StudyStatusEntity.ARCHIVED);
        studyModuleRepository.update(archivedStudyModuleEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<StudyModuleReadDTO> actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeStudyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(archivedStudyModuleEntity.getStudyElementId())),
            hasProperty("studyElementId", is(cancelledStudyModuleEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindStudyModules_returnsSubElementHierarchy_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/studymodules";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "TUNI");
        CooperationNetworkEntity coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetworkEntity coopNetwork2 = createCooperationNetwork("CN-2");

        StudyModuleEntity activeStudyModuleEntity = createStudyModule("SM1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork);

        StudyElementReferenceEntity studyModuleReference = EntityInitializer.getStudyElementReferenceForStudyModule("SM1", "UEF");

        // denormalized realisation info
        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatusEntity.ACTIVE);

        CourseUnitRealisationEntity organiserOutsideOfNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("TUNI");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setRealisationId("TOT-2");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setStatus(StudyStatusEntity.ACTIVE);

        // courseunit
        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), null, null, null, Collections.singletonList("fi"));
        courseUnitEntity.setParents(Collections.singletonList(studyModuleReference));
        courseUnitEntity.setRealisations(Arrays.asList(activeCourseUnitRealisationEntity, organiserOutsideOfNetworkCourseUnitRealisationEntity));
        courseUnitRepository.update(courseUnitEntity);

        StudyElementReferenceEntity courseUnitReference = EntityInitializer.getStudyElementReferenceForCourseUnit("CU1", "UEF");

        // actual realisations
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(courseUnitReference), Collections.singletonList(coopNetwork));
        RealisationEntity organiserOutsideOfNetworkRealisationEntity = createRealisation("TOT-2", "TUNI",
            Collections.singletonList(courseUnitReference), Collections.singletonList(coopNetwork2));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<StudyModuleReadDTO> actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeStudyModuleEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        assertEquals(1, actualResult.get(0).getSubElements().size());
        assertEquals(courseUnitEntity.getStudyElementId(), actualResult.get(0).getSubElements().get(0).getStudyElementId());

        CourseUnitReadDTO subElementCourseUnit = (CourseUnitReadDTO) actualResult.get(0).getSubElements().get(0);
        assertEquals(1, subElementCourseUnit.getRealisations().size());
        assertEquals(activeRealisationEntity.getRealisationId(), subElementCourseUnit.getRealisations().get(0).getRealisationId());
    }
}
