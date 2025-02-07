package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.List;

public interface StudentRepositoryExtended {

    List<StudentEntity> findByOidOrPersonId(@Nullable String oid, @Nullable String personId, Pageable pageable);
}
