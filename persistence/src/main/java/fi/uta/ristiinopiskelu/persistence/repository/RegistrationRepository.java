package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends ExtendedRepository<RegistrationEntity, String>, RegistrationRepositoryExtended {

    Optional<List<RegistrationEntity>> findAllByStudentPersonIdAndStudentOid(String studentPersonId, String studentOid, Pageable pageable);

    Optional<List<RegistrationEntity>> findAllByStudentPersonIdOrStudentOid(String studentPersonId, String studentOid, Pageable pageable);

    Optional<List<RegistrationEntity>> findAllByStudentPersonId(String studentPersonId, Pageable pageable);
    
    Optional<List<RegistrationEntity>> findAllByStudentOid(String studentOid, Pageable pageable);

    Optional<List<RegistrationEntity>> findByStudentHomeStudyRightIdentifiersStudyRightIdAndStudentHomeStudyRightIdentifiersOrganisationTkCodeReference(
        String studyRightId, String organisation);
}
