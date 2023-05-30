package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;

import java.util.Optional;

public interface RealisationRepository extends ExtendedRepository<RealisationEntity, String>, CommonStudyRepository<RealisationEntity>, RealisationRepositoryExtended {

    Optional<RealisationEntity> findByRealisationIdAndOrganizingOrganisationId(String id, String organizingOrganisationId);

    void deleteByRealisationIdAndOrganizingOrganisationId(String id, String organizingOrganisationId);
}
