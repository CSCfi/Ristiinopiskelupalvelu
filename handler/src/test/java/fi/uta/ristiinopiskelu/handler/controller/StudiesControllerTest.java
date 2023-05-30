package fi.uta.ristiinopiskelu.handler.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CourseUnitReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.realisation.RealisationSearchResults;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchResults;
import fi.uta.ristiinopiskelu.handler.controller.v9.StudiesControllerV9;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.StudiesService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudiesControllerV9.class)
@Import(ElasticsearchConfigurationSupport.class)
public class StudiesControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseUnitService courseUnitService;

    @MockBean
    private StudyModuleService studyModuleService;

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private StudiesService studiesService;

    @Test
    public void testGETfindRealisations_realisationIdGiven_shouldReturn200() throws Exception {
        when(realisationService.searchByIds(any(), any(), any(), any(), any())).thenReturn(new RealisationSearchResults());
        this.mockMvc.perform(
                get("/api/v9/studies/realisations")
                        .header("eppn", "testailija@testailija.fi")
                        .header("SSL_CLIENT_S_DN_O", "UEF")
                        .param("organizingOrganisationId", "UEF")
                        .param("realisationId", "RID1"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testPOSTfindRealisations_allParametersGiven_shouldSuccessInParsingParametersAndReturn200() throws Exception {
        when(realisationService.search(any(), any())).thenReturn(new RealisationSearchResults());

        RealisationSearchParameters params = new RealisationSearchParameters();
        params.setOrganizingOrganisationId("TUNI");
        params.setRealisationIdentifierCode("RCODE");
        params.setRealisationId("R1");
        params.setNetworkIdentifiers(Arrays.asList("CN1", "CN2"));
        params.setQuery("TEST");
        params.setCourseUnitReferences(Arrays.asList(new CourseUnitReference("C1", "UEF"),
                new CourseUnitReference(null, "UEF"),
                new CourseUnitReference("C1", "UEF")));

        this.mockMvc.perform(
                post("/api/v9/studies/realisations/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params))
                        .header("eppn", "testailija@testailija.fi")
                        .header("SSL_CLIENT_S_DN_O", "UEF"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testPOSTsearch_allDeprecatedParametersGiven_shouldSuccessInParsingParametersAndReturn200() throws Exception {
        when(studiesService.search(any(), any())).thenReturn(new StudiesSearchResults());

        StudiesSearchParameters params = new StudiesSearchParameters();
        params.setNetworkIdentifiers(Arrays.asList("CN1", "CN2"));
        params.setQuery("TEST");
        params.setOrganizingOrganisationIdentifiers(Arrays.asList("ORG1", "ORG2"));
        params.setLanguage(Language.FI);
        params.setRealisationStartDate(LocalDate.now().minusMonths(1));
        params.setRealisationEndDate(LocalDate.now().plusMonths(1));
        params.setRealisationEnrollmentStartDateTime(OffsetDateTime.now());
        params.setRealisationEnrollmentEndDateTime(OffsetDateTime.now().plusMonths(1));

        this.mockMvc.perform(
            post("/api/v9/studies/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params))
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF"))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    public void testPOSTsearch_allParametersGiven_shouldSuccessInParsingParametersAndReturn200() throws Exception {
        when(studiesService.search(any(), any())).thenReturn(new StudiesSearchResults());

        StudiesSearchParameters params = new StudiesSearchParameters();
        params.setNetworkIdentifiers(Arrays.asList("CN1", "CN2"));
        params.setQuery("TEST");
        params.setOrganizingOrganisationIdentifiers(Arrays.asList("ORG1", "ORG2"));
        params.setLanguage(Language.FI);
        params.setRealisationStartDate(LocalDate.now().minusMonths(1));
        params.setRealisationEndDate(LocalDate.now().plusMonths(1));
        params.setRealisationEnrollmentStartDateTimeTo(OffsetDateTime.now());
        params.setRealisationEnrollmentEndDateTimeTo(OffsetDateTime.now().plusMonths(1));

        this.mockMvc.perform(
                post("/api/v9/studies/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params))
                        .header("eppn", "testailija@testailija.fi")
                        .header("SSL_CLIENT_S_DN_O", "UEF"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testPOSTsearch_allParametersGivenAsJsonNode_shouldSuccessInParsingParametersAndReturn200() throws Exception {
        when(studiesService.search(any(), any())).thenReturn(new StudiesSearchResults());

        JsonNode node = objectMapper.createObjectNode();
        ((ObjectNode)node).put("query", "TEST");
        ((ObjectNode)node).putArray("networkIdentifiers").add("CN1");
        ((ObjectNode)node).putArray("organizingOrganisationIdentifiers").add("ORG1");
        ((ObjectNode)node).put("type", "COURSE_UNIT");
        ((ObjectNode)node).put("realisationStartDate","2020-01-01");
        ((ObjectNode)node).put("realisationEndDate","2020-02-01");
        ((ObjectNode)node).put("realisationEnrollmentStartDateTime","2020-01-01T00:00:00.000+02:00");
        ((ObjectNode)node).put("realisationEnrollmentEndDateTime","2020-02-01T00:00:00.000+02:00");

        this.mockMvc.perform(
            post("/api/v9/studies/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(node))
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF"))
            .andDo(print())
            .andExpect(status().isOk());
    }
}
