package fi.uta.ristiinopiskelu.handler.service;

import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit.CourseUnitReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.exception.CreateFailedException;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;

import java.util.List;
import java.util.Optional;

public interface CourseUnitService extends StudyElementService<CourseUnitWriteDTO, CourseUnitEntity, CourseUnitReadDTO> {
    
    List<CompositeIdentifiedEntityModificationResult> createAll(List<CourseUnitWriteDTO> studyElements, String organisationId) throws CreateFailedException;

    Optional<CourseUnitEntity> findByIdAndAssessmentItemIdAndOrganizer(String id, String assessmentItemId, String organizingOrganisationId);
}
