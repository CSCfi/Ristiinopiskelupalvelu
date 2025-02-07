package fi.uta.ristiinopiskelu.handler.integration.controller.current;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudiesControllerV9CourseUnitIntegrationTest extends AbstractStudiesControllerV9IntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity activeCourseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity cancelledCourseUnitEntity = createCourseUnit("CU2", "Peruttu Metro", "UEF",
            "Jarh", allNetworks, null, null, null, Collections.singletonList("fi"));
        cancelledCourseUnitEntity.setStatus(StudyStatus.CANCELLED);
        courseUnitRepository.update(cancelledCourseUnitEntity);

        CourseUnitEntity archivedCourseUnitEntity = createCourseUnit("CU3", "Arkistoitu Metro", "UEF",
            "Rooeh", allNetworks, null, null, null, Collections.singletonList("fi"));
        archivedCourseUnitEntity.setStatus(StudyStatus.ARCHIVED);
        courseUnitRepository.update(archivedCourseUnitEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledCourseUnitEntity.getStudyElementId(), actualResult.get(0).getStudyElementId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(archivedCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(cancelledCourseUnitEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindCourseUnits_returnsCourseUnitsWithValidRealisationsOnly_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CooperationNetwork outdatedNetworkRef = DtoInitializer.getCooperationNetwork("CN-1", null, true,
            LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));

        CooperationNetwork otherNetwork = createCooperationNetwork("CN-2");

        CourseUnitRealisationEntity networkRefOutdatedCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        networkRefOutdatedCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        networkRefOutdatedCourseUnitRealisationEntity.setRealisationId("TOT-2");
        networkRefOutdatedCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(outdatedNetworkRef));
        networkRefOutdatedCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity statusCancelledCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        statusCancelledCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        statusCancelledCourseUnitRealisationEntity.setRealisationId("TOT-3");
        statusCancelledCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        statusCancelledCourseUnitRealisationEntity.setStatus(StudyStatus.CANCELLED);

        CourseUnitRealisationEntity otherNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        otherNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        otherNetworkCourseUnitRealisationEntity.setRealisationId("TOT-4");
        otherNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(otherNetwork));
        otherNetworkCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        List<CourseUnitRealisationEntity> allRealisations = Arrays.asList(activeCourseUnitRealisationEntity, networkRefOutdatedCourseUnitRealisationEntity,
            statusCancelledCourseUnitRealisationEntity, otherNetworkCourseUnitRealisationEntity);

        CourseUnitEntity activeCourseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, allRealisations, null, null, Collections.singletonList("fi"));

        CourseUnitEntity networkRefOutdatedCourseUnitEntity = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(outdatedNetworkRef), null, null, null, Collections.singletonList("fi"));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(networkRefOutdatedCourseUnitEntity.getStudyElementId()))
        ));

        CourseUnitReadDTO activeCourseUnit = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(activeCourseUnitEntity.getStudyElementId()))
            .findAny().get();

        // remember, /studies/courseunits "includeInactive" default value is true. "otherNetworkCourseUnitRealisationEntity" is
        // only returned here because it is the organisations own realisation, although not being in organisation's networks.
        assertEquals(4, activeCourseUnit.getRealisations().size());
        assertThat(activeCourseUnit.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(activeCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(statusCancelledCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(networkRefOutdatedCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(otherNetworkCourseUnitRealisationEntity.getRealisationId()))
        ));
        
        // no inactives
        requestParams.add("includeInactive", "false");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(activeCourseUnitEntity.getStudyElementId()))
        ));

        activeCourseUnit = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(activeCourseUnitEntity.getStudyElementId()))
            .findAny().get();

        assertEquals(2, activeCourseUnit.getRealisations().size());
        assertThat(activeCourseUnit.getRealisations(), containsInAnyOrder(
            hasProperty("realisationId", is(activeCourseUnitRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(statusCancelledCourseUnitRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindCourseUnitRealisations_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        String url = String.format("/api/v9/studies/courseunits/%s/realisations", courseUnitEntity.getStudyElementId());

        StudyElementReference reference = new StudyElementReference("CU1", "UEF", StudyElementType.COURSE_UNIT);

        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF", Collections.singletonList(reference), allNetworks);

        RealisationEntity cancelledRealisationEntity = createRealisation("TOT-2", "UEF", Collections.singletonList(reference), allNetworks);
        cancelledRealisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(cancelledRealisationEntity);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF", Collections.singletonList(reference), allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only archived
        requestParams.add("statuses", "ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only cancelled
        requestParams.clear();
        requestParams.add("statuses", "CANCELLED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // all statuses
        requestParams.clear();
        requestParams.add("statuses", "ACTIVE,CANCELLED,ARCHIVED");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(activeRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(archivedRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(cancelledRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindCourseUnits_pagingWorksAsIntended_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        CourseUnitEntity courseUnit1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit3 = createCourseUnit("CU3", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit4 = createCourseUnit("CU4", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        CourseUnitEntity courseUnit5 = createCourseUnit("CU5", "Testi Metro", "UEF",
            "Raipatirai", allNetworks, null, null, null, Collections.singletonList("fi"));

        // get only 1 for each page
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("includeInactive", "true");
        requestParams.set("page", "0");
        requestParams.set("pageSize", "1");

        List<String> fetchedIds = new ArrayList<>();

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "1");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "2");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "3");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page
        requestParams.set("page", "4");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertFalse(fetchedIds.contains(actualResult.get(0).getStudyElementId()));
        fetchedIds.add(actualResult.get(0).getStudyElementId());

        // next page, now should return no results
        requestParams.set("page", "5");
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());

        // GOTTA CATCH 'EM ALL
        assertEquals(5, fetchedIds.size());
        assertThat(fetchedIds, containsInAnyOrder(
            courseUnit1.getStudyElementId(),
            courseUnit2.getStudyElementId(),
            courseUnit3.getStudyElementId(),
            courseUnit4.getStudyElementId(),
            courseUnit5.getStudyElementId()
        ));
    }

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2= createCooperationNetwork("CN-2");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork2, null, BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // get only 1 for each page
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("includeInactive", "true");

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        requestParams.set("organizingOrganisationId", "JYU");
        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindCourseUnits_returnsOnlyCourseUnitRealisationsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "UEF", "JYU");
        createNetworkWithOrganisations("CN-3", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");
        CooperationNetwork coopNetwork3 = createCooperationNetwork("CN-3");

        CourseUnitRealisationEntity courseUnitRealisationEntityOutsideOfOwnNetworks = new CourseUnitRealisationEntity();
        courseUnitRealisationEntityOutsideOfOwnNetworks.setRealisationId("R1");
        courseUnitRealisationEntityOutsideOfOwnNetworks.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntityOutsideOfOwnNetworks.setCooperationNetworks(Collections.singletonList(coopNetwork3));
        courseUnitRealisationEntityOutsideOfOwnNetworks.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity courseUnitRealisationEntityInOwnNetwork = new CourseUnitRealisationEntity();
        courseUnitRealisationEntityInOwnNetwork.setRealisationId("R1");
        courseUnitRealisationEntityInOwnNetwork.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntityInOwnNetwork.setCooperationNetworks(Collections.singletonList(coopNetwork));
        courseUnitRealisationEntityInOwnNetwork.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, Collections.singletonList(courseUnitRealisationEntityOutsideOfOwnNetworks), BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, Collections.singletonList(courseUnitRealisationEntityInOwnNetwork), BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork2, Collections.emptyList(), BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // first without params
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity3.getStudyElementId()))
        ));

        // "courseUnitRealisationEntityOutsideOfOwnNetworks" is only returned here because it's the organistion's own realisation and includeInactive = true by default
        CourseUnitReadDTO courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityOutsideOfOwnNetworks.getRealisationId(), courseUnit1Dto.getRealisations().get(0).getRealisationId());
        
        CourseUnitReadDTO courseUnit2Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity2.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit2Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityInOwnNetwork.getRealisationId(), courseUnit2Dto.getRealisations().get(0).getRealisationId());

        CourseUnitReadDTO courseUnit3Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity3.getStudyElementId())).findFirst().get();
        assertEquals(0, courseUnit3Dto.getRealisations().size());

        // then with courseUnitId and organizingOrganisationId params. result should be the same.
        requestParams.add("courseUnitId", "CU1");
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());
        assertEquals(courseUnitRealisationEntityOutsideOfOwnNetworks.getRealisationId(), courseUnit1Dto.getRealisations().get(0).getRealisationId());
    }

    // NOTE: At the time of this writing (9/22) this isn't a valid real life case anymore (hasn't been for a long time). No subelements can be set to COURSE_UNITs currently.
    @Test
    public void testFindCourseUnits_returnsSubElementHierarchy_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "TUNI");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        CourseUnitEntity courseUnitEntity1 = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        StudyElementReference courseUnit1Reference = new StudyElementReference("CU1", "UEF", StudyElementType.COURSE_UNIT);

        // denormalized realisation info
        CourseUnitRealisationEntity activeCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        activeCourseUnitRealisationEntity.setOrganizingOrganisationId("UEF");
        activeCourseUnitRealisationEntity.setRealisationId("TOT-1");
        activeCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork));
        activeCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity organiserOutsideOfNetworkCourseUnitRealisationEntity = new CourseUnitRealisationEntity();
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setOrganizingOrganisationId("TUNI");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setRealisationId("TOT-2");
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setCooperationNetworks(Collections.singletonList(coopNetwork2));
        organiserOutsideOfNetworkCourseUnitRealisationEntity.setStatus(StudyStatus.ACTIVE);

        // courseunit
        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), null, null, null, Collections.singletonList("fi"));
        courseUnitEntity2.setParents(Collections.singletonList(courseUnit1Reference));
        courseUnitEntity2.setRealisations(Arrays.asList(activeCourseUnitRealisationEntity, organiserOutsideOfNetworkCourseUnitRealisationEntity));
        courseUnitRepository.update(courseUnitEntity2);

        StudyElementReference courseUnit2Reference = new StudyElementReference("CU2", "UEF", StudyElementType.COURSE_UNIT);

        // actual realisations
        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            Collections.singletonList(courseUnit2Reference), Collections.singletonList(coopNetwork));
        RealisationEntity organiserOutsideOfNetworkRealisationEntity = createRealisation("TOT-2", "TUNI",
            Collections.singletonList(courseUnit2Reference), Collections.singletonList(coopNetwork2));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        // we now get both courseunits in the result list, filter out other than the one that has subelements
        assertEquals(2, actualResult.size());

        actualResult = actualResult.stream().filter(r -> r.getStudyElementId().equalsIgnoreCase(courseUnitEntity1.getStudyElementId())).collect(Collectors.toList());
        assertEquals(1, actualResult.get(0).getSubElements().size());
        assertEquals(courseUnitEntity2.getStudyElementId(), actualResult.get(0).getSubElements().get(0).getStudyElementId());

        CourseUnitReadDTO subElementCourseUnit = (CourseUnitReadDTO) actualResult.get(0).getSubElements().get(0);
        assertEquals(1, subElementCourseUnit.getRealisations().size());
        assertEquals(activeRealisationEntity.getRealisationId(), subElementCourseUnit.getRealisations().get(0).getRealisationId());
    }

    @Test
    public void testFindCourseUnits_returnsOnlyOwnRealisationsFromUnpublishedNetwork_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        NetworkEntity networkEntity = createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        networkEntity.setPublished(false);
        networkRepository.update(networkEntity);

        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        
        CourseUnitRealisationEntity courseUnitRealisationEntity1 = new CourseUnitRealisationEntity();
        courseUnitRealisationEntity1.setRealisationId("R1");
        courseUnitRealisationEntity1.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntity1.setCooperationNetworks(Collections.singletonList(coopNetwork));
        courseUnitRealisationEntity1.setStatus(StudyStatus.ACTIVE);

        CourseUnitRealisationEntity courseUnitRealisationEntity2 = new CourseUnitRealisationEntity();
        courseUnitRealisationEntity2.setRealisationId("R2");
        courseUnitRealisationEntity2.setOrganizingOrganisationId("TUNI");
        courseUnitRealisationEntity2.setCooperationNetworks(Collections.singletonList(coopNetwork));
        courseUnitRealisationEntity2.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", coopNetwork, Collections.singletonList(courseUnitRealisationEntity1), BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", coopNetwork, Collections.singletonList(courseUnitRealisationEntity2), BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", coopNetwork, Collections.emptyList(), BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // first without params
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        CourseUnitReadDTO courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());

        CourseUnitReadDTO courseUnit2Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity2.getStudyElementId())).findFirst().get();
        assertEquals(0, courseUnit2Dto.getRealisations().size());

        // then with courseUnitId and organizingOrganisationId params
        requestParams.add("courseUnitId", "CU1");
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));

        courseUnit1Dto = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();
        assertEquals(1, courseUnit1Dto.getRealisations().size());
    }

    @Test
    public void testFindCourseUnits_returnsOwnCourseUnitsIfNotMemberOfAnyNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        assertEquals(0, networkRepository.findAll(Pageable.unpaged()).getTotalElements());

        CourseUnitRealisationEntity courseUnitRealisationEntity1 = new CourseUnitRealisationEntity();
        courseUnitRealisationEntity1.setRealisationId("R1");
        courseUnitRealisationEntity1.setOrganizingOrganisationId("UEF");
        courseUnitRealisationEntity1.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.emptyList(), Collections.singletonList(courseUnitRealisationEntity1), BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "UEF",
            "Keppihevonen", Collections.emptyList(), null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", Collections.emptyList(), Collections.emptyList(), BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // first without params
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId())),
            hasProperty("studyElementId", is(courseUnitEntity2.getStudyElementId()))
        ));

        CourseUnitReadDTO cu1 = actualResult.stream().filter(cu -> cu.getStudyElementId().equals(courseUnitEntity.getStudyElementId())).findFirst().get();

        assertEquals(1, cu1.getRealisations().size());
        assertEquals(courseUnitRealisationEntity1.getRealisationId(), cu1.getRealisations().get(0).getRealisationId());

        // then with courseUnitId and organizingOrganisationId params
        requestParams.add("courseUnitId", "CU1");
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));
    }

    @Test
    public void testFindCourseUnits_returnsOwnCourseUnitsOutsideOfOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/courseunits";

        createNetworkWithOrganisations("CN-1", "TUNI", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        CourseUnitEntity courseUnitEntity = createCourseUnit("CU1", "Testi Metro", "UEF",
            "Raipatirai", Collections.singletonList(coopNetwork), null, BigDecimal.valueOf(1), BigDecimal.valueOf(5));

        CourseUnitEntity courseUnitEntity2 = createCourseUnit("CU2", "JUY matikka", "TUNI",
            "Keppihevonen", Collections.singletonList(coopNetwork), null, BigDecimal.valueOf(2), BigDecimal.valueOf(3));

        CourseUnitEntity courseUnitEntity3 = createCourseUnit("CU3", "JAR matikka", "JYU",
            "Keppivirtahepo", Collections.singletonList(coopNetwork), Collections.emptyList(), BigDecimal.valueOf(6), BigDecimal.valueOf(10));

        // first without params (includeInactive = true)
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();

        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<CourseUnitReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("studyElementId", is(courseUnitEntity.getStudyElementId()))
        ));
    }
}
