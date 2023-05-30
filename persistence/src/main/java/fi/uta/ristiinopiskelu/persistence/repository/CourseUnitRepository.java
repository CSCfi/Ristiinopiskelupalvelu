package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;

public interface CourseUnitRepository extends StudyElementRepository<CourseUnitEntity>, CourseUnitRepositoryExtended, CommonStudyRepository<CourseUnitEntity> {
}


