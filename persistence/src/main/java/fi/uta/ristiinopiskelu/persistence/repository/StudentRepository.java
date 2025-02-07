package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;

public interface StudentRepository extends ExtendedRepository<StudentEntity, String>, StudentRepositoryExtended {
}
