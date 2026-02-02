package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;

import java.util.List;

public interface RegistrationRepository extends ExtendedRepository<RegistrationEntity, String>, RegistrationRepositoryExtended {

    List<RegistrationEntity> findByStudentHomeStudyRightIdentifiersStudyRightIdAndStudentHomeStudyRightIdentifiersOrganisationTkCodeReference(
        String studyRightId, String organisation);
}
