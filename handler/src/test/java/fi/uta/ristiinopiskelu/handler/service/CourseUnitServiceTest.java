package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.config.MapperConfig;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.AssessmentItemWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CompletionOptionWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.service.impl.CourseUnitServiceImpl;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperConfig.class)
public class CourseUnitServiceTest {

    @MockBean
    private CourseUnitRepository courseUnitRepository;

    @MockBean
    private RealisationRepository realisationRepository;

    @Autowired
    private ModelMapper modelMapper;

    private ModelMapper modelMapperSpy;

    private CourseUnitService courseUnitService;

    @BeforeEach
    private void beforeEach() {
        modelMapperSpy = spy(modelMapper);
        courseUnitService = spy(new CourseUnitServiceImpl(courseUnitRepository, realisationRepository, modelMapperSpy));
    }

    @Test
    public void testCreateAll_noRealisations_shouldSuccess() {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN1", null, true, LocalDate.now().minusMonths(1), null);

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("CU1", "CODE", new LocalisedString("Jakso", null, null),
            Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference), new BigDecimal(5), new BigDecimal(5));

        doReturn(Optional.empty()).when(courseUnitRepository).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        doReturn(modelMapper.map(courseUnit, CourseUnitEntity.class)).when(courseUnitService).create(any());

        CompositeIdentifiedEntityModificationResult result = courseUnitService.createAll(Collections.singletonList(courseUnit), organizingOrganisationId);

        assertEquals(1, result.getCreatedAmount(CompositeIdentifiedEntityType.COURSE_UNIT));
    }

    @Test
    public void testCreateAll_courseUnitWithRealisations_shouldSuccess() {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN1", null, true, LocalDate.now().minusMonths(1), null);

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCourseUnit("CU1", "CODE", new LocalisedString("Jakso", null, null),
            Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference), new BigDecimal(5), new BigDecimal(5));

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("R1", "RCODE1", new LocalisedString("Toteutus 1", null, null),
            null, Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference));

        courseUnit.setRealisations(Collections.singletonList(realisation));

        StudyElementReference existingReference = new StudyElementReference(courseUnit.getStudyElementId(),
            organizingOrganisationId, StudyElementType.ASSESSMENT_ITEM, "AI1");

        RealisationWriteDTO aiRealisation = DtoInitializer.getRealisation("R2", "RCODE2", new LocalisedString("Toteutus 2", null, null),
            null, Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference));

        aiRealisation.setStudyElementReferences(Collections.singletonList(existingReference));

        AssessmentItemWriteDTO ai = DtoInitializer.getAssessmentItem(existingReference.getReferenceAssessmentItemId(), new LocalisedString("Arvioinninkohde", null, null));
        ai.setRealisations(Collections.singletonList(aiRealisation));

        CompletionOptionWriteDTO completionOption = new CompletionOptionWriteDTO();
        completionOption.setAssessmentItems(Collections.singletonList(ai));

        courseUnit.setCompletionOptions(Collections.singletonList(completionOption));

        RealisationEntity realisationEntity = modelMapper.map(realisation, RealisationEntity.class);
        RealisationEntity aiRealisationEntity = modelMapper.map(aiRealisation, RealisationEntity.class);
        Optional optionalAiRealisationEntity = Optional.of(aiRealisationEntity);

        CourseUnitEntity courseUnitEntity = modelMapper.map(courseUnit, CourseUnitEntity.class);

        doReturn(Optional.empty()).when(courseUnitRepository).findByStudyElementIdAndOrganizingOrganisationId(any(), any());
        doReturn(Optional.empty()).when(realisationRepository).findByRealisationIdAndOrganizingOrganisationId(eq(realisation.getRealisationId()), any());
        doReturn(optionalAiRealisationEntity).when(realisationRepository).findByRealisationIdAndOrganizingOrganisationId(eq(aiRealisation.getRealisationId()), any()); // return AI realisation as "existing"
        doReturn(realisationEntity).when(realisationRepository).create(any());
        doReturn("historyid").when(realisationRepository).saveHistory(any(), any());
        doReturn(optionalAiRealisationEntity.get()).when(realisationRepository).update(any());
        doReturn(realisationEntity).when(modelMapperSpy).map(eq(realisation), eq(RealisationEntity.class));
        //noinspection unchecked
        doReturn(courseUnitEntity).when(courseUnitService).create(any());
        doReturn(courseUnitEntity).when(courseUnitRepository).create(any());
        doReturn(courseUnitEntity).when(modelMapperSpy).map(eq(courseUnit), eq(CourseUnitEntity.class));

        CompositeIdentifiedEntityModificationResult result = courseUnitService.createAll(Collections.singletonList(courseUnit), organizingOrganisationId);

        assertEquals(1, result.getCreatedAmount(CompositeIdentifiedEntityType.COURSE_UNIT));
        assertEquals(1, result.getCreatedAmount(CompositeIdentifiedEntityType.REALISATION));
        assertEquals(1, result.getUpdatedAmount(CompositeIdentifiedEntityType.REALISATION));

        RealisationEntity createdRealisation = (RealisationEntity) result.getCreated().stream()
            .filter(entity -> entity.getType() == CompositeIdentifiedEntityType.REALISATION).findFirst().orElse(null);

        // Assert organizingOrganisation and status set to created realisation
        assertEquals(organizingOrganisationId, createdRealisation.getOrganizingOrganisationId());
        assertEquals(StudyStatus.ACTIVE, createdRealisation.getStatus());

        // Verify reference added
        assertEquals(1, createdRealisation.getStudyElementReferences().size());
        assertEquals(new StudyElementReference(courseUnit.getStudyElementId(), organizingOrganisationId, StudyElementType.COURSE_UNIT), createdRealisation.getStudyElementReferences().get(0));

        // Assert no extra reference added for updated realisation
        RealisationEntity updatedRealisation = (RealisationEntity) result.getCreated().stream()
            .filter(entity -> entity.getType() == CompositeIdentifiedEntityType.REALISATION).findFirst().orElse(null);
        assertNotNull(updatedRealisation);
        assertEquals(aiRealisation.getStudyElementReferences().size(), updatedRealisation.getStudyElementReferences().size());

    }

}
