package fi.uta.ristiinopiskelu.persistence.repository;

import fi.uta.ristiinopiskelu.datamodel.entity.CodeEntity;

import java.util.stream.Stream;

public interface CodeRepository extends ExtendedRepository<CodeEntity, String>, CodeRepositoryExtended {

    Stream<CodeEntity> findAllByCodeSetKeyOrderByCodeUri(String codeSetKey);

    Stream<CodeEntity> findAllByKeyAndCodeSetKeyOrderByCodeUri(String key, String codeSetKey);
}
