package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;

import java.util.List;

public interface StudentRepository extends ExtendedRepository<StudentEntity, String> {

    List<StudentEntity> findByOidOrPersonIdOrderByTimestampDesc(String oid, String personId);
}
