package fi.uta.ristiinopiskelu.handler.integration.controller.v8;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.network.Network;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
public class NetworkControllerV8IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private NetworkRepository networkRepository;

    @Test
    public void testFindNetworkById_shouldSucceed() throws Exception {
        NetworkOrganisationEntity org = new NetworkOrganisationEntity();
        org.setOrganisationTkCode("UEF");

        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("NETWORK1", new LocalisedStringEntity("Testiverkosto", null, null),
            Collections.singletonList(org), EntityInitializer.getIndefinitelyValidity(OffsetDateTime.now()), true);
        networkRepository.create(networkEntity);

        MvcResult result = this.mockMvc.perform(
            get("/api/v8/networks/NETWORK1")
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF"))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.print("Content:\n" + content);

        List<Network> results = Arrays.asList(jsonMapper.readValue(content, Network[].class));
        assertEquals(1, results.size());
        assertEquals(networkEntity.getId(), results.get(0).getId());
        assertEquals(networkEntity.isPublished(), results.get(0).getPublished());
    }
}
