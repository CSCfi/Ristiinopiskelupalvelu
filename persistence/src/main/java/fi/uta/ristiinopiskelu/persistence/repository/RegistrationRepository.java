package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;

import java.util.stream.Stream;

public interface RegistrationRepository extends ExtendedRepository<RegistrationEntity, String>, RegistrationRepositoryExtended {

    Stream<RegistrationEntity> findByStudentHomeStudyRightIdentifiersStudyRightIdAndStudentHomeStudyRightIdentifiersOrganisationTkCodeReference(
        String studyRightId, String organisation);
}
