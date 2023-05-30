package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudyRecordRepository extends ExtendedRepository<StudyRecordEntity, String>, StudyRecordRepositoryExtended {
    List<StudyRecordEntity> findAllByStudentOidOrStudentHomeEppn(String studentOid, String studentHomeEppn, Pageable pageable);
}
