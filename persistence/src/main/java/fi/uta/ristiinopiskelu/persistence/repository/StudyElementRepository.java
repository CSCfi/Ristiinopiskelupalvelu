package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface StudyElementRepository<T extends StudyElementEntity> extends CommonStudyRepository<T>, ExtendedRepository<T, String> {

    Optional<List<T>> findByOrganizingOrganisationIdIn(List<String> organizingOrganisationIds);

    Optional<T> findByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId);

    void deleteByStudyElementIdAndOrganizingOrganisationId(String studyElementId, String organizingOrganisationId);

    Optional<List<T>> findByIdInAndOrganizingOrganisationIdIn(List<String> ids, List<String> organizingOrganisationIds);

    Optional<List<T>> findByIdInAndCooperationNetworksIdIn(List<String> ids, List<String> cooperationNetworkIds);

    Optional<List<T>> findByCooperationNetworksIdIn(List<String> cooperationNetworkIds);
}
