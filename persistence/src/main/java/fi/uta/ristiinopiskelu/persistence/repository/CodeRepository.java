package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;

import java.util.List;

public interface CodeRepository extends ExtendedRepository<CodeEntity, String>, CodeRepositoryExtended {

    List<CodeEntity> findAllByCodeSetKeyOrderByCodeUri(String codeSetKey);

    List<CodeEntity> findAllByKeyAndCodeSetKeyOrderByCodeUri(String key, String codeSetKey);
}
