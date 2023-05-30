package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;

import java.util.Optional;

public interface CourseUnitRepositoryExtended {
    Optional<CourseUnitEntity> findByIdAndAssessmentItemIdAndOrganizer(String id, String assessmentItemId, String organizingOrganisationId);
}
