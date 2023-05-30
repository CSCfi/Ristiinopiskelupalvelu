package fi.uta.ristiinopiskelu.handler.integration.controller.current;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.realisation.RealisationReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudiesControllerV9RealisationIntegrationTest extends AbstractStudiesControllerV9IntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Test
    public void testFindRealisation_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        RealisationEntity realisationEntity = createRealisation("TOT-1", "UEF", null, allNetworks);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.set("realisationId", "TOT-1");
        requestParams.set("organizingOrganisationId", "UEF");
        MvcResult result = getMvcResult(url, requestParams);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // change realisation status to CANCELLED
        realisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(realisationEntity);

        // now it should not be visible anymore with default params
        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());

        // change status param and it should become visible again
        requestParams.set("statuses", "CANCELLED");

        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // and with all params too
        requestParams.set("statuses", "ACTIVE,ARCHIVED,CANCELLED");

        result = getMvcResult(url, requestParams);
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());
    }

    @Test
    public void testFindRealisation_returnsAllRealisations_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "HAAGAH");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2 = createCooperationNetwork("CN-2");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "HAAGAH", null, Collections.singletonList(coopNetwork2));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity5 = createRealisation("TOT-5", "UEF", null, Collections.singletonList(coopNetwork));

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(4, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity5.getRealisationId()))
        ));
    }

    @Test
    public void testFindRealisations_returnsOnlyRealisationsWithActiveStatusByDefault_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations/search";

        createNetworkWithOrganisations("CN-1", "UEF");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        List<CooperationNetwork> allNetworks = new ArrayList<>();
        allNetworks.add(coopNetwork);

        RealisationEntity activeRealisationEntity = createRealisation("TOT-1", "UEF",
            null, allNetworks);

        RealisationEntity cancelledRealisationEntity = createRealisation("TOT-2", "UEF",
            null, allNetworks);
        cancelledRealisationEntity.setStatus(StudyStatus.CANCELLED);
        realisationRepository.update(cancelledRealisationEntity);

        RealisationEntity archivedRealisationEntity = createRealisation("TOT-3", "UEF",
            null, allNetworks);
        archivedRealisationEntity.setStatus(StudyStatus.ARCHIVED);
        realisationRepository.update(archivedRealisationEntity);

        // just default values
        RealisationSearchParameters searchParams = new RealisationSearchParameters();
        searchParams.setIncludeInactive(true);
        searchParams.setIncludeOwn(true);
        searchParams.setIncludePast(true);
        searchParams.setOngoingEnrollment(false);

        MvcResult result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(activeRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only archived
        searchParams.setStatuses(Collections.singletonList(StudyStatus.ARCHIVED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(archivedRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // only cancelled
        searchParams.setStatuses(Collections.singletonList(StudyStatus.CANCELLED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(cancelledRealisationEntity.getRealisationId(), actualResult.get(0).getRealisationId());

        // all statuses
        searchParams.setStatuses(Arrays.asList(StudyStatus.ACTIVE, StudyStatus.CANCELLED, StudyStatus.ARCHIVED));
        result = getMvcResult(url, objectMapper.writeValueAsString(searchParams));

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(activeRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(archivedRealisationEntity.getRealisationId())),
            hasProperty("realisationId", is(cancelledRealisationEntity.getRealisationId()))
        ));
    }

    @Test
    public void testFindRealisations_returnsOnlyRealisationsInOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations/search";

        createNetworkWithOrganisations("CN-1", "UEF");
        createNetworkWithOrganisations("CN-2", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");
        CooperationNetwork coopNetwork2= createCooperationNetwork("CN-2");

        RealisationEntity realisationEntity = createRealisation("R1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("R2", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity3 = createRealisation("R3", "JYU", null, Collections.singletonList(coopNetwork2));

        RealisationSearchParameters searchParams = new RealisationSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOngoingEnrollment(false);

        MvcResult result = this.getMvcResult(url, objectMapper.writeValueAsString(searchParams));
        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));

        searchParams = new RealisationSearchParameters();
        searchParams.setIncludeOwn(true);
        searchParams.setOngoingEnrollment(false);
        searchParams.setNetworkIdentifiers(Arrays.asList("CN-2"));

        result = this.getMvcResult(url, objectMapper.writeValueAsString(searchParams));
        content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindRealisation_returnsRealisationsCorrectlyFromOwnNetworks_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF", "HAAGAH", "TUNI", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "HAAGAH", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams);

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(4, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // UEF's realisation1 as HAAGAH
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity1.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "HAAGAH");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId()))
        ));

        // JYU's realisation4 as TUNI
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity4.getRealisationId());
        requestParams.add("organizingOrganisationId", "JYU");

        result = getMvcResult(url, requestParams, "TUNI");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // TUNI's realisation4 as JYU
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity3.getRealisationId());
        requestParams.add("organizingOrganisationId", "TUNI");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity3.getRealisationId()))
        ));

        // HAAGAH's realisation4 as UEF
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity2.getRealisationId());
        requestParams.add("organizingOrganisationId", "HAAGAH");

        result = getMvcResult(url, requestParams, "UEF");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));
    }

    @Test
    public void testFindRealisation_returnsOwnRealisationsWithoutNetworksCorrectly_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations";

        createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF", null, null);
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(4, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // just default values as TUNI
        requestParams = new LinkedMultiValueMap<>();

        result = getMvcResult(url, requestParams, "TUNI");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity3.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity4.getRealisationId()))
        ));

        // UEF's realisation2 as JYU
        requestParams = new LinkedMultiValueMap<>();
        requestParams.add("realisationId", realisationEntity2.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());
    }

    @Test
    public void testFindRealisation_returnsOnlyOwnRealisationsFromUnpublishedNetwork_shouldSucceed() throws Exception {
        String url = "/api/v9/studies/realisations";

        NetworkEntity networkEntity = createNetworkWithOrganisations("CN-1", "UEF", "TUNI", "JYU");
        networkEntity.setPublished(false);
        networkRepository.update(networkEntity);

        CooperationNetwork coopNetwork = createCooperationNetwork("CN-1");

        RealisationEntity realisationEntity1 = createRealisation("TOT-1", "UEF", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity2 = createRealisation("TOT-2", "UEF", null, null);
        RealisationEntity realisationEntity3 = createRealisation("TOT-3", "TUNI", null, Collections.singletonList(coopNetwork));
        RealisationEntity realisationEntity4 = createRealisation("TOT-4", "JYU", null, Collections.singletonList(coopNetwork));

        // just default values as UEF
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(url, requestParams, "UEF");

        List<RealisationReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("realisationId", is(realisationEntity1.getRealisationId())),
            hasProperty("realisationId", is(realisationEntity2.getRealisationId()))
        ));

        // just default values as TUNI
        result = getMvcResult(url, requestParams, "TUNI");
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity3.getRealisationId(), actualResult.get(0).getRealisationId());

        // just default values as JYU
        result = getMvcResult(url, requestParams, "JYU");
        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertEquals(realisationEntity4.getRealisationId(), actualResult.get(0).getRealisationId());

        // UEF's realisation2 as JYU
        requestParams.add("realisationId", realisationEntity2.getRealisationId());
        requestParams.add("organizingOrganisationId", "UEF");

        result = getMvcResult(url, requestParams, "JYU");

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(0, actualResult.size());
    }
}
