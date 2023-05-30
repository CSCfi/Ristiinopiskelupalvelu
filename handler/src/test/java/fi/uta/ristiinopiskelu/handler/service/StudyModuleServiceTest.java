package fi.uta.ristiinopiskelu.handler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule.StudyModuleReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyelement.studymodule.StudyModuleSearchParameters;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
public class StudyModuleServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceTest.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private StudyModuleService studyModuleService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${general.messageSchema.version}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }

        if(!networkService.findById("CN-1").isPresent()) {
            Validity validity = new Validity();
            validity.setContinuity(Validity.ContinuityEnum.FIXED);
            validity.setStart(OffsetDateTime.now().minusYears(1));
            validity.setEnd(OffsetDateTime.now().plusYears(2));

            List<NetworkOrganisation> networkOrganisations = organisationIds.stream()
                .map(orgId -> new NetworkOrganisation(orgId, true, validity)).collect(Collectors.toList());
            NetworkEntity networkEntity = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("Verkosto", null, null),
                networkOrganisations, validity, true);
            networkService.create(networkEntity);
        }
    }

    @Test
    public void testSearchingStudyModuleWithSubElements() {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        LocalisedString courseUnitName = new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null);
        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("CU1", "CU1-CODE1", courseUnitName,
            Collections.singletonList(network), Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5.0));
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnit.setParents(Collections.singletonList(new StudyElementReference("SM1",
            organisingOrganisationId, StudyElementType.STUDY_MODULE)));

        StudyModuleWriteDTO studyModule = DtoInitializer.getStudyModule("SM1", "SM1-CODE1",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ACTIVE);

        CourseUnitEntity courseUnitEntity = courseUnitService.create(modelMapper.map(courseUnit, CourseUnitEntity.class));
        StudyModuleEntity studyModuleEntity = studyModuleService.create(modelMapper.map(studyModule, StudyModuleEntity.class));

        assertEquals(courseUnit.getStudyElementId(), courseUnitEntity.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), courseUnitEntity.getStudyElementIdentifierCode());
        assertEquals(studyModule.getStudyElementId(), studyModuleEntity.getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), studyModuleEntity.getStudyElementIdentifierCode());

        List<StudyModuleReadDTO> results = studyModuleService.search(organisingOrganisationId,
            new StudyModuleSearchParameters("SM1", "SM1-CODE1",
                organisingOrganisationId, null, true, 0, 1000)).getResults();

        assertFalse(CollectionUtils.isEmpty(results));
        assertEquals(1, results.size());
        assertEquals(studyModule.getStudyElementId(), results.get(0).getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), results.get(0).getStudyElementIdentifierCode());
        assertFalse(CollectionUtils.isEmpty(results.get(0).getSubElements()));
        assertEquals(1, results.get(0).getSubElements().size());
        assertEquals(courseUnit.getStudyElementId(), results.get(0).getSubElements().get(0).getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), results.get(0).getSubElements().get(0).getStudyElementIdentifierCode());
    }

    @Test
    public void testSearchingInactiveStudyModule_networkReferenceOutdated() {
        String organisingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(3), LocalDate.now().minusYears(2));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getStudyModule("SM1", "SM1-CODE1",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ACTIVE);

        StudyModuleEntity studyModuleEntity = studyModuleService.create(modelMapper.map(studyModule, StudyModuleEntity.class));

        assertEquals(studyModule.getStudyElementId(), studyModuleEntity.getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), studyModuleEntity.getStudyElementIdentifierCode());

        List<StudyModuleReadDTO> results = studyModuleService.search(organisingOrganisationId,
            new StudyModuleSearchParameters("SM1", "SM1-CODE1",
                organisingOrganisationId, null, false, 0, 1000)).getResults();

        assertTrue(CollectionUtils.isEmpty(results));

        results = studyModuleService.search(organisingOrganisationId,
            new StudyModuleSearchParameters("SM1", "SM1-CODE1",
                organisingOrganisationId, null, true, 0, 1000)).getResults();

        assertFalse(CollectionUtils.isEmpty(results));
        assertEquals(1, results.size());
        assertEquals(studyModule.getStudyElementId(), results.get(0).getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), results.get(0).getStudyElementIdentifierCode());
    }

    @Test
    public void testSearchingInactiveStudyModule_studyElementValidityOutdated() {
        String organisingOrganisationId = "TUNI";
        
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(2));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getStudyModule("SM1", "SM1-CODE1",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);
        studyModule.setStatus(StudyStatus.ACTIVE);
        studyModule.setValidityStartDate(LocalDate.now().minusYears(2));
        studyModule.setValidityEndDate(LocalDate.now().minusYears(1));

        StudyModuleEntity studyModuleEntity = studyModuleService.create(modelMapper.map(studyModule, StudyModuleEntity.class));

        assertEquals(studyModule.getStudyElementId(), studyModuleEntity.getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), studyModuleEntity.getStudyElementIdentifierCode());

        List<StudyModuleReadDTO> results = studyModuleService.search(organisingOrganisationId,
            new StudyModuleSearchParameters("SM1", "SM1-CODE1",
                organisingOrganisationId, null, false, 0, 1000)).getResults();

        assertTrue(CollectionUtils.isEmpty(results));

        results = studyModuleService.search(organisingOrganisationId,
            new StudyModuleSearchParameters("SM1", "SM1-CODE1",
                organisingOrganisationId, null, true, 0, 1000)).getResults();

        assertFalse(CollectionUtils.isEmpty(results));
        assertEquals(1, results.size());
        assertEquals(studyModule.getStudyElementId(), results.get(0).getStudyElementId());
        assertEquals(studyModule.getStudyElementIdentifierCode(), results.get(0).getStudyElementIdentifierCode());
    }
}
