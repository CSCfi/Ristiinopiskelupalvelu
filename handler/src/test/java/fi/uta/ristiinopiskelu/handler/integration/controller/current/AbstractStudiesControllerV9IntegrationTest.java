package fi.uta.ristiinopiskelu.handler.integration.controller.current;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studies.StudiesSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TODO: @DirtiesContext is a quick'n'dirty fix here, should _NOT_ be necessary. Removing it will make
 * StudyRecordRouteIntegrationTest#testSendingCreateStudyRecordMessageForCourseUnit_withOnlyTestPersonsAllowed_shouldFail() test case
 * fail mystically when running *all* tests in handler. Probably something to do with MockMvc use here, all controller integration
 * tests should probably be refactored to use TestRestTemplate or something similar that does actual requests. Just a hunch though.
 */
@DirtiesContext
public class AbstractStudiesControllerV9IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    protected MvcResult getMvcResult(StudiesSearchParameters searchParams) throws Exception {
        return this.getMvcResult("/api/v9/studies/search", objectMapper.writeValueAsString(searchParams));
    }

    protected MvcResult getMvcResult(String url, MultiValueMap<String, String> params, String org) throws Exception {
        return this.mockMvc.perform(
                get(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", org)
                    .params(params)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }

    protected MvcResult getMvcResult(String url, MultiValueMap<String, String> params) throws Exception {
        return getMvcResult(url, params, "UEF");
    }

    protected MvcResult getMvcResult(String url, String content) throws Exception {
        return this.mockMvc.perform(
                post(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", "UEF")
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }

    protected NetworkEntity createNetworkWithOrganisations(String networkId, String... organisationTkCodes) {
        Validity validity = new Validity();
        validity.setContinuity(Validity.ContinuityEnum.FIXED);
        validity.setStart(OffsetDateTime.now().minusYears(1));
        validity.setEnd(OffsetDateTime.now().plusYears(2));

        List<NetworkOrganisation> networkOrganisations = Arrays.asList(organisationTkCodes)
            .stream().map(organisationTkCode -> new NetworkOrganisation(organisationTkCode, true, validity)).collect(Collectors.toList());
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(networkId, null, networkOrganisations, validity, true);

        networkRepository.create(networkEntity);

        return networkEntity;
    }

    protected CooperationNetwork createCooperationNetwork(String networkId) {
        return DtoInitializer.getCooperationNetwork(networkId, null, true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(2));
    }

    protected CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, CooperationNetwork coopNetwork, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax) {
        return this.createCourseUnit(courseUnitId, identifierCode, organisationTkCode, name, Collections.singletonList(coopNetwork), realisationRefs, creditsMin, creditsMax);
    }

    protected CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, List<CooperationNetwork> coopNetworks, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax) {
        return this.createCourseUnit(courseUnitId, identifierCode, organisationTkCode, name, coopNetworks, realisationRefs, creditsMin, creditsMax, null);
    }

    protected CourseUnitEntity createCourseUnit(String courseUnitId, String identifierCode, String organisationTkCode,
                                              String name, List<CooperationNetwork> coopNetworks, List<CourseUnitRealisationEntity> realisationRefs, BigDecimal creditsMin, BigDecimal creditsMax,
                                              List<String> teachingLanguages) {
        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity(courseUnitId, organisationTkCode, coopNetworks,
            new LocalisedString(name, null, null));
        courseUnitEntity.setStudyElementIdentifierCode(identifierCode);
        courseUnitEntity.setOrganisationReferences(
            Collections.singletonList(DtoInitializer.getOrganisationReference(DtoInitializer.getOrganisation(organisationTkCode, organisationTkCode), OrganisationRole.ROLE_MAIN_ORGANIZER)));
        courseUnitEntity.setRealisations(realisationRefs);
        courseUnitEntity.setCreditsMax(creditsMax);
        courseUnitEntity.setCreditsMin(creditsMin);
        courseUnitEntity.setTeachingLanguage(teachingLanguages);
        courseUnitEntity.setOrganizingOrganisationId(organisationTkCode);
        courseUnitEntity.setStatus(StudyStatus.ACTIVE);

        courseUnitRepository.create(courseUnitEntity);
        return courseUnitEntity;
    }

    protected StudyModuleEntity createStudyModule(String studyModuleId, String identifierCode, String organisationTkCode, String name, CooperationNetwork coopNetwork) {
        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(studyModuleId, identifierCode, organisationTkCode, Collections.singletonList(coopNetwork),
            new LocalisedString(name, null, null));
        studyModuleEntity.setOrganisationReferences(
            Collections.singletonList(DtoInitializer.getOrganisationReference(DtoInitializer.getOrganisation(organisationTkCode, organisationTkCode), OrganisationRole.ROLE_MAIN_ORGANIZER)));

        studyModuleRepository.create(studyModuleEntity);
        return studyModuleEntity;
    }

    protected RealisationEntity createRealisation(String realisationId, String organizingOrganisationTkCode, List<StudyElementReference> studyElements, List<CooperationNetwork> cooperationNetworks) {
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity(realisationId, organizingOrganisationTkCode, studyElements, cooperationNetworks);
        realisationRepository.create(realisationEntity);
        return realisationEntity;
    }
}
