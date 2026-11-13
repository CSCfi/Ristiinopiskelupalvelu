package fi.uta.ristiinopiskelu.handler.integration.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Organisation;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({
    EmbeddedActiveMQInitializer.class,
    EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class OrganisationControllerV8IntegrationTest {

    private static final String API_URL = "/api/v8/organisations";

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testFindAllOrganisations_shouldSucceed() throws Exception {
        OrganisationEntity organisation1 = EntityInitializer.getOrganisationEntity("ORG1", "org1-queue", new LocalisedStringEntity("Organisaatio 1"), 9);
        OrganisationEntity organisation2 = EntityInitializer.getOrganisationEntity("ORG2", "org2-queue", new LocalisedStringEntity("Organisaatio 2"), 9);
        OrganisationEntity organisation3 = EntityInitializer.getOrganisationEntity("ORG3", "org3-queue", new LocalisedStringEntity("Organisaatio 3"), 9);
        organisationRepository.save(organisation1);
        organisationRepository.save(organisation2);
        organisationRepository.save(organisation3);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult(API_URL, requestParams);

        List<Organisation> actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(3, actualResult.size());
        assertThat(actualResult).extracting("organisationTkCode").contains(
            organisation1.getId(), organisation2.getId(), organisation3.getId()
        );
        assertThat(actualResult).extracting("organisationName")
            .extracting(v -> ((fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString)v).getValue(fi.uta.ristiinopiskelu.datamodel.dto.v8.Language.FI))
            .containsExactlyInAnyOrder(
                organisation1.getOrganisationName().getValue(Language.FI),
                organisation2.getOrganisationName().getValue(Language.FI),
                organisation3.getOrganisationName().getValue(Language.FI)
            );
    }

    @Test
    public void testFindOrganisationByOrganisationTkCode_shouldSucceed() throws Exception {
        OrganisationEntity organisation1 = EntityInitializer.getOrganisationEntity("ORG1", "org1-queue", new LocalisedStringEntity("Organisaatio 1"), 9);
        OrganisationEntity organisation2 = EntityInitializer.getOrganisationEntity("ORG2", "org2-queue", new LocalisedStringEntity("Organisaatio 2"), 9);
        OrganisationEntity organisation3 = EntityInitializer.getOrganisationEntity("ORG3", "org3-queue", new LocalisedStringEntity("Organisaatio 3"), 9);
        organisationRepository.save(organisation1);
        organisationRepository.save(organisation2);
        organisationRepository.save(organisation3);

        // just default values
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        MvcResult result = getMvcResult("%s/%s".formatted(API_URL, organisation3.getId()), requestParams);

        List<Organisation> actualResult = jsonMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertEquals(1, actualResult.size());
        assertThat(actualResult).extracting("organisationTkCode").contains(
            organisation3.getId()
        );
        assertThat(actualResult).extracting("organisationName")
            .extracting(v -> ((fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString)v).getValue(fi.uta.ristiinopiskelu.datamodel.dto.v8.Language.FI))
            .containsExactlyInAnyOrder(
                organisation3.getOrganisationName().getValue(Language.FI)
            );
    }

    private MvcResult getMvcResult(String url, MultiValueMap<String, String> params) throws Exception {
        return this.mockMvc.perform(
                get(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", "UEF")
                    .params(params)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }
}
